<#
  Pacote LGPD essencial para o Delphix — Data Control Tower (DCT) ou Continuous Compliance Engine.

    powershell -ExecutionPolicy Bypass -File .\importar-lgpd.ps1

  14 classes de dados (documentos, nomes, contato, endereço, CEP e data de nascimento), 30 classifiers
  e a política de descoberta "BR - LGPD - essencial". CPF, CNPJ e telefone usam os algoritmos do
  plugin BR (BR-java17.jar, nesta pasta): no DCT, se o plugin faltar, o script oferece instalá-lo.

  Pergunta o idioma, o destino, o endereço e as credenciais; lista o que já existe com os mesmos
  nomes, APAGA antes de importar (o que o Delphix recusar apagar é atualizado) e grava
  import-<data>.log nesta pasta, sem segredos. Windows PowerShell 5.1 ou PowerShell 7.
  Baseado no import.ps1 dos pacotes do Delphix Masking Helper.
#>

Set-StrictMode -Version 2
$ErrorActionPreference = 'Stop'

$Here = $PSScriptRoot
$Data = Join-Path $Here 'data'
$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$LogFile = Join-Path $Here "import-$Stamp.log"
$Utf8 = New-Object System.Text.UTF8Encoding($false)
$Col = 0

# ── Messages ──────────────────────────────────────────────────────────────────
# English, Português, Español; {0}, {1} are the values.
$Messages = @{
  'title'          = @('Load "{0}" (version {1}) into Delphix', 'Carregar "{0}" (versão {1}) no Delphix', 'Cargar "{0}" (versión {1}) en Delphix')
  'target'         = @('Where should it be loaded?', 'Onde carregar?', '¿Dónde cargarla?')
  'target_dct'     = @('Data Control Tower (DCT)', 'Data Control Tower (DCT)', 'Data Control Tower (DCT)')
  'target_engine'  = @('Continuous Compliance Engine', 'Continuous Compliance Engine', 'Continuous Compliance Engine')
  'choice'         = @('Choice', 'Opção', 'Opción')
  'host_dct'       = @('DCT address (host name or URL)', 'Endereço do DCT (host ou URL)', 'Dirección de DCT (host o URL)')
  'host_engine'    = @('Engine address (host name or URL)', 'Endereço do engine (host ou URL)', 'Dirección del engine (host o URL)')
  'apikey'         = @('DCT API key', 'Chave de API do DCT', 'Clave de API de DCT')
  'user'           = @('User', 'Usuário', 'Usuario')
  'password'       = @('Password', 'Senha', 'Contraseña')
  'insecure'       = @('Accept a self-signed or expired certificate? (y/N)', 'Aceitar certificado autoassinado ou expirado? (s/N)', '¿Aceptar un certificado autofirmado o vencido? (s/N)')
  'pack'           = @('Which pack?', 'Qual pacote?', '¿Qué paquete?')
  'pack_essential' = @('Essential — the minimum for the law ({0} data classes, {1} classifiers)', 'Essencial — o mínimo para a lei ({0} classes de dados, {1} classifiers)', 'Esencial — lo mínimo para la ley ({0} clases de datos, {1} classifiers)')
  'pack_extended'  = @('Extended — everything ({0} data classes, {1} classifiers)', 'Estendido — tudo ({0} classes de dados, {1} classifiers)', 'Extendido — todo ({0} clases de datos, {1} classifiers)')
  'connecting'     = @('Connecting to {0}…', 'Conectando a {0}…', 'Conectando a {0}…')
  'connected'      = @('Connected.', 'Conectado.', 'Conectado.')
  'connect_fail'   = @('Could not connect: {0}', 'Não foi possível conectar: {0}', 'No se pudo conectar: {0}')
  'reading'        = @('Reading what is already there…', 'Lendo o que já existe lá…', 'Leyendo lo que ya existe allí…')
  'nothing_exists' = @('None of these objects exists there yet.', 'Nenhum destes objetos existe lá ainda.', 'Ninguno de estos objetos existe allí todavía.')
  'warn_title'     = @('WARNING: these objects already exist and will be DELETED before the import:', 'ATENÇÃO: estes objetos já existem e serão APAGADOS antes da importação:', 'ATENCIÓN: estos objetos ya existen y se BORRARÁN antes de la importación:')
  'warn_update'    = @('Whatever Delphix refuses to delete — because something else uses it — is updated instead.', 'O que o Delphix se recusar a apagar — porque algo mais o usa — é atualizado em vez disso.', 'Lo que Delphix se niegue a borrar — porque algo más lo usa — se actualiza en su lugar.')
  'confirm'        = @('Continue? (y/N)', 'Continuar? (s/N)', '¿Continuar? (s/N)')
  'cancelled'      = @('Cancelled. Nothing was changed.', 'Cancelado. Nada foi alterado.', 'Cancelado. No se cambió nada.')
  'kind_pol'       = @('discovery policy', 'política de descoberta', 'política de descubrimiento')
  'kind_cls'       = @('classifier', 'classifier', 'classifier')
  'kind_dc'        = @('data class', 'classe de dados', 'clase de datos')
  'kind_alg'       = @('algorithm', 'algoritmo', 'algoritmo')
  'deleting'       = @('Deleting {0} {1}', 'Apagando {0} {1}', 'Borrando {0} {1}')
  'deleted'        = @('deleted', 'apagado', 'borrado')
  'kept'           = @('refused ({0}) — it will be updated', 'recusado ({0}) — será atualizado', 'rechazado ({0}) — se actualizará')
  'creating'       = @('Creating {0} {1}', 'Criando {0} {1}', 'Creando {0} {1}')
  'updating'       = @('Updating {0} {1}', 'Atualizando {0} {1}', 'Actualizando {0} {1}')
  'ok'             = @('ok', 'ok', 'ok')
  'unchanged'      = @('already as sent — left as it is', 'já está como seria enviado — mantido', 'ya está como se enviaría — se mantiene')
  'failed'         = @('FAILED: {0}', 'FALHOU: {0}', 'FALLÓ: {0}')
  'checkdigit'     = @('Data Control Tower refuses to update Checkdigit algorithms (HTTP 500); delete "{0}" there and run again.', 'O Data Control Tower recusa atualizar algoritmos Checkdigit (HTTP 500); apague "{0}" lá e rode de novo.', 'Data Control Tower se niega a actualizar algoritmos Checkdigit (HTTP 500); borra "{0}" allí y ejecuta de nuevo.')
  'no_framework'   = @('Delphix has no "{0}" framework in its dlpx-core plugin.', 'O Delphix não tem o framework "{0}" no plugin dlpx-core.', 'Delphix no tiene el framework "{0}" en su plugin dlpx-core.')
  'no_builtin'     = @('Delphix has no built-in algorithm "{0}".', 'O Delphix não tem o algoritmo de fábrica "{0}".', 'Delphix no tiene el algoritmo de fábrica "{0}".')
  'stopped'        = @('The import stopped. What was done so far stays in Delphix; running again resumes it.', 'A importação parou. O que já foi feito fica no Delphix; rodar de novo retoma.', 'La importación se detuvo. Lo hecho hasta aquí queda en Delphix; ejecutar de nuevo la retoma.')
  'done'           = @('Done: {0} created, {1} updated, {2} deleted before the import.', 'Concluído: {0} criados, {1} atualizados, {2} apagados antes da importação.', 'Listo: {0} creados, {1} actualizados, {2} borrados antes de la importación.')
  'log'            = @('Log: {0}', 'Log: {0}', 'Log: {0}')
  'plugin_ok'      = @('Plugin {0} {1} is there, with {2}.', 'Plugin {0} {1} instalado, com {2}.', 'Plugin {0} {1} instalado, con {2}.')
  'plugin_missing' = @('Delphix lacks the {0} plugin (algorithms {1}).', 'O Delphix não tem o plugin {0} (algoritmos {1}).', 'Delphix no tiene el plugin {0} (algoritmos {1}).')
  'plugin_ask'     = @('Install {0} now? (Y/n)', 'Instalar {0} agora? (S/n)', '¿Instalar {0} ahora? (S/n)')
  'plugin_sending' = @('Sending {0}…', 'Enviando {0}…', 'Enviando {0}…')
  'plugin_install' = @('Installing the plugin…', 'Instalando o plugin…', 'Instalando el plugin…')
  'plugin_done'    = @('Plugin {0} installed.', 'Plugin {0} instalado.', 'Plugin {0} instalado.')
  'plugin_nojar'   = @('Install the {0} plugin there (BR-java17.jar) and run again.', 'Instale o plugin {0} lá (BR-java17.jar) e rode de novo.', 'Instala el plugin {0} allí (BR-java17.jar) y ejecuta de nuevo.')
  'plugin_engine'  = @('Install the {0} plugin on the engine (BR-java17.jar) and run again.', 'Instale o plugin {0} no engine (BR-java17.jar) e rode de novo.', 'Instala el plugin {0} en el engine (BR-java17.jar) y ejecuta de nuevo.')
}

function Msg([string]$Key) {
  $text = $Messages[$Key][$script:Col]
  if ($args.Count -gt 0) { return [string]::Format($text, [object[]]$args) }
  return $text
}
function Write-Log([string]$Text) {
  [System.IO.File]::AppendAllText($LogFile, ((Get-Date -Format 'HH:mm:ss') + ' ' + $Text + "`r`n"), $Utf8)
}

# Answers come from the console — or, unattended, from the file DLPX_IMPORT_ANSWERS names.
$Answers = $null
if ($env:DLPX_IMPORT_ANSWERS) {
  $Answers = New-Object System.Collections.Queue
  foreach ($line in [System.IO.File]::ReadAllLines($env:DLPX_IMPORT_ANSWERS)) { $Answers.Enqueue($line) }
}
function Ask([string]$Prompt, [string]$Default = '') {
  $label = $Prompt
  if ($Default) { $label = "$Prompt [$Default]" }
  if ($null -ne $Answers) {
    $answer = ''
    if ($Answers.Count -gt 0) { $answer = [string]$Answers.Dequeue() }
    Write-Host "${label}: $answer"
  } else {
    $answer = Read-Host $label
  }
  if (-not $answer) { $answer = $Default }
  return $answer
}
function Ask-Secret([string]$Prompt) {
  if ($null -ne $Answers) {
    $answer = ''
    if ($Answers.Count -gt 0) { $answer = [string]$Answers.Dequeue() }
    Write-Host "${Prompt}: ********"
    return $answer
  }
  $secure = Read-Host $Prompt -AsSecureString
  $ptr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  try { return [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
  finally { [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}
function Test-Yes([string]$Answer) { return $Answer -match '^(y|yes|s|sim|si|sí)$' }

# ── Language ──────────────────────────────────────────────────────────────────

Write-Host ''
Write-Host '  1) English'
Write-Host '  2) Português'
Write-Host '  3) Español'
Write-Host ''
switch (Ask 'Language / Idioma' '1') { '2' { $Col = 1 } '3' { $Col = 2 } default { $Col = 0 } }

$Index = [System.IO.File]::ReadAllText((Join-Path $Data 'index.json'), $Utf8) | ConvertFrom-Json
$Locale = @('en', 'pt-BR', 'es')[$Col]
$Name = $Index.name.en
if ($Index.name.PSObject.Properties[$Locale]) { $Name = $Index.name.PSObject.Properties[$Locale].Value }
Write-Log "Delphix discovery policy import — $($Index.id) v$($Index.version)"
Write-Host ''
Write-Host (Msg 'title' $Name $Index.version)
Write-Host ''

# ── Where, how, which pack ────────────────────────────────────────────────────

Write-Host (Msg 'target')
Write-Host ('  1) ' + (Msg 'target_dct'))
Write-Host ('  2) ' + (Msg 'target_engine'))
$Target = 'dct'
if ((Ask (Msg 'choice') '1') -eq '2') { $Target = 'engine' }
Write-Log "target: $Target"

$Key = ''; $UserName = ''; $Password = ''
if ($Target -eq 'dct') {
  $HostName = Ask (Msg 'host_dct')
  $Key = (Ask-Secret (Msg 'apikey')) -replace '^(apk|APK)\s+', ''
} else {
  $HostName = Ask (Msg 'host_engine')
  $UserName = Ask (Msg 'user') 'admin'
  $Password = Ask-Secret (Msg 'password')
}
$Insecure = Test-Yes (Ask (Msg 'insecure') 'n')

$Base = $HostName.Trim()
if ($Base -notmatch '^https?://') { $Base = 'https://' + $Base }
$Base = $Base.TrimEnd('/')
if ($Target -eq 'dct') {
  if ($Base -match '/dct/v3$') { } elseif ($Base -match '/dct$') { $Base += '/v3' } else { $Base += '/dct/v3' }
} else {
  if ($Base -notmatch '/masking/api$') { $Base += '/masking/api' }
}
Write-Log "api: $Base (self-signed accepted: $Insecure)"

$Pack = 'extended'
if ($Index.packs.PSObject.Properties['essential']) {
  Write-Host ''
  Write-Host (Msg 'pack')
  $essentialDc = @($Index.packs.essential.dataClasses).Count; $essentialCls = @($Index.packs.essential.classifiers).Count
  $extendedDc = @($Index.packs.extended.dataClasses).Count; $extendedCls = @($Index.packs.extended.classifiers).Count
  Write-Host ('  1) ' + (Msg 'pack_essential' $essentialDc $essentialCls))
  Write-Host ('  2) ' + (Msg 'pack_extended' $extendedDc $extendedCls))
  if ((Ask (Msg 'choice') '2') -eq '1') { $Pack = 'essential' }
}
Write-Log "pack: $Pack"
$PackLists = $Index.packs.PSObject.Properties[$Pack].Value
$Algorithms = @($Index.algorithms | Where-Object { @($PackLists.algorithms) -contains $_.name })
$DataClasses = @($Index.dataClasses | Where-Object { @($PackLists.dataClasses) -contains $_.name })
$Classifiers = @($Index.classifiers | Where-Object { @($PackLists.classifiers) -contains $_.name })
$PolicyName = $Index.policy.name
$PolicyFile = $Index.policy.files.PSObject.Properties[$Pack].Value

# ── HTTP ──────────────────────────────────────────────────────────────────────

[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor [System.Net.SecurityProtocolType]::Tls12
# PowerShell 5.1 accepts a script-block callback. PowerShell 7 runs it on a thread with no
# runspace, so use a compiled callback there.
$script:DlpxTrustAllCallback = $null
if ($Insecure) {
  if ($PSVersionTable.PSVersion.Major -ge 7) {
    if (-not ('DlpxTrustAll' -as [type])) {
      Add-Type -TypeDefinition @'
using System.Net.Security;
using System.Security.Cryptography.X509Certificates;
public static class DlpxTrustAll {
  public static bool Accept(object sender, X509Certificate cert, X509Chain chain, SslPolicyErrors errors) { return true; }
  public static RemoteCertificateValidationCallback Callback { get { return new RemoteCertificateValidationCallback(Accept); } }
}
'@
    }
    $script:DlpxTrustAllCallback = [DlpxTrustAll]::Callback
  } else {
    $script:DlpxTrustAllCallback = [System.Net.Security.RemoteCertificateValidationCallback]{
      param($sender, $cert, $chain, $errors)
      return $true
    }
  }
  [System.Net.ServicePointManager]::ServerCertificateValidationCallback = $script:DlpxTrustAllCallback
}

$script:AuthHeader = ''
function Invoke-Api([string]$Method, [string]$Path, [byte[]]$Body = $null, [string]$ContentType = 'application/json') {
  $attempt = 1
  while ($true) {
    $req = [System.Net.HttpWebRequest]::Create($script:Base + $Path)
    $req.Method = $Method
    $req.Accept = 'application/json'
    $req.Timeout = 300000
    $req.ReadWriteTimeout = 300000
    if ($Insecure) { $req.ServerCertificateValidationCallback = $script:DlpxTrustAllCallback }
    if ($script:AuthHeader) { $req.Headers.Add('Authorization', $script:AuthHeader) }
    $status = 0; $text = ''; $resp = $null
    try {
      if ($null -ne $Body) {
        $req.ContentType = $ContentType
        $req.ContentLength = $Body.Length
        $stream = $req.GetRequestStream(); $stream.Write($Body, 0, $Body.Length); $stream.Close()
      }
      $resp = $req.GetResponse()
    } catch [System.Net.WebException] {
      $resp = $_.Exception.Response
      if ($null -eq $resp) { $text = $_.Exception.Message }
    } catch {
      $text = $_.Exception.Message
    }
    if ($null -ne $resp) {
      $status = [int]$resp.StatusCode
      $reader = New-Object System.IO.StreamReader($resp.GetResponseStream(), $Utf8)
      $text = $reader.ReadToEnd(); $reader.Close(); $resp.Close()
    }
    Write-Log "  $Method $Path → $status"
    # An engine forgets a session after a while: log in again once.
    if ($status -eq 401 -and $Target -eq 'engine' -and $attempt -eq 1 -and $Path -ne '/login') {
      if (Connect-Engine) { $attempt = 2; continue }
    }
    # A read can be asked again when the server was busy; a write never is.
    if ($Method -eq 'GET' -and $attempt -lt 3 -and ($status -eq 0 -or ($status -ge 502 -and $status -le 504))) {
      $attempt++; Start-Sleep -Seconds 3; continue
    }
    return New-Object PSObject -Property @{ Status = $status; Text = $text }
  }
}
function Test-Ok($r) { return $r.Status -ge 200 -and $r.Status -lt 300 }
function From-Json($r) { if ($r.Text) { return $r.Text | ConvertFrom-Json } return $null }
function Get-ErrorText($r) {
  if ($r.Status -eq 0) { return $r.Text }
  try {
    $o = $r.Text | ConvertFrom-Json
    foreach ($field in @('error_description', 'errorMessage', 'detail', 'message')) {
      if ($o.PSObject.Properties[$field] -and $o.$field) { return [string]$o.$field }
    }
    if ($o.PSObject.Properties['errors'] -and $o.errors) { return (@($o.errors) -join '; ') }
  } catch { }
  if ($r.Text) { return $r.Text.Substring(0, [Math]::Min(300, $r.Text.Length)) }
  return "HTTP $($r.Status)"
}
function Json-Bytes($value) { return $Utf8.GetBytes(($value | ConvertTo-Json -Depth 100 -Compress)) }

function Connect-Engine {
  $script:AuthHeader = ''
  $r = Invoke-Api 'POST' '/login' (Json-Bytes (New-Object PSObject -Property @{ username = $UserName; password = $Password }))
  if (-not (Test-Ok $r)) { $script:LastResponse = $r; return $false }
  $script:AuthHeader = (From-Json $r).Authorization
  return [bool]$script:AuthHeader
}

# Waits for what a write started: an engine's async task, or a DCT job. Throws when it failed.
function Wait-Done($r) {
  $o = From-Json $r
  if ($null -eq $o) { return }
  if ($Target -eq 'dct') {
    if (-not $o.PSObject.Properties['job'] -or -not $o.job) { return }
    $id = $o.job.id
    for ($i = 0; $i -lt 900; $i++) {
      $j = From-Json (Invoke-Api 'GET' "/jobs/$id")
      switch ($j.status) {
        'COMPLETED' { return }
        { $_ -in @('FAILED', 'CANCELED', 'TIMEDOUT', 'ABANDONED', 'SUSPENDED') } {
          $detail = $j.status
          if ($j.PSObject.Properties['error_details'] -and $j.error_details) { $detail = $j.error_details }
          throw $detail
        }
      }
      Start-Sleep -Seconds 1
    }
    throw "job $id still running"
  } else {
    if (-not $o.PSObject.Properties['asyncTaskId']) { return }
    $id = $o.asyncTaskId
    for ($i = 0; $i -lt 900; $i++) {
      $t = From-Json (Invoke-Api 'GET' "/async-tasks/$id")
      if ($t.status -eq 'SUCCEEDED') { return }
      if ($t.status -eq 'FAILED' -or $t.status -eq 'CANCELLED') {
        $detail = $t.status
        if ($t.PSObject.Properties['exceptionDetail'] -and $t.exceptionDetail) { $detail = $t.exceptionDetail }
        throw $detail
      }
      Start-Sleep -Seconds 1
    }
    throw "task $id still running"
  }
}

# Every item of a list. Small pages keep each answer well inside what Windows PowerShell 5.1 parses.
function Get-All([string]$Path) {
  $all = New-Object System.Collections.ArrayList
  $sep = '?'; if ($Path.Contains('?')) { $sep = '&' }
  if ($Target -eq 'dct') {
    $cursor = ''
    while ($true) {
      $url = "$Path${sep}limit=100"
      if ($cursor) { $url += '&cursor=' + [System.Uri]::EscapeDataString($cursor) }
      $r = Invoke-Api 'GET' $url
      if (-not (Test-Ok $r)) { throw (Get-ErrorText $r) }
      $o = From-Json $r
      foreach ($x in @($o.items)) { if ($null -ne $x) { [void]$all.Add($x) } }
      $cursor = ''
      if ($o.PSObject.Properties['response_metadata'] -and $o.response_metadata.PSObject.Properties['next_cursor']) { $cursor = $o.response_metadata.next_cursor }
      if (-not $cursor) { break }
    }
  } else {
    $page = 1
    while ($true) {
      $r = Invoke-Api 'GET' "$Path${sep}page_size=100&page_number=$page"
      if (-not (Test-Ok $r)) { throw (Get-ErrorText $r) }
      $o = From-Json $r
      $items = @($o.responseList)
      foreach ($x in $items) { if ($null -ne $x) { [void]$all.Add($x) } }
      $total = 0; if ($o.PSObject.Properties['_pageInfo']) { $total = [int]$o._pageInfo.total }
      if ($items.Count -eq 0 -or $all.Count -ge $total) { break }
      $page++
    }
  }
  return ,$all
}

# ── Connect ───────────────────────────────────────────────────────────────────

Write-Host ''
Write-Host (Msg 'connecting' $Base)
if ($Target -eq 'dct') {
  $script:AuthHeader = "apk $Key"
  $r = Invoke-Api 'GET' '/algorithms?limit=1'
  if (-not (Test-Ok $r)) { Write-Host (Msg 'connect_fail' (Get-ErrorText $r)); Write-Log 'connect failed'; exit 1 }
} else {
  if (-not (Connect-Engine)) { Write-Host (Msg 'connect_fail' (Get-ErrorText $script:LastResponse)); Write-Log 'connect failed'; exit 1 }
}
Write-Host (Msg 'connected')

# ── What is there ─────────────────────────────────────────────────────────────

Write-Host (Msg 'reading')
$State = @{ alg = @{}; dc = @{}; cls = @{}; files = @{}; update = @{} }
$Frameworks = @{}; $ClassifierFrameworks = @{}
$Own = @{ alg = @{}; dc = @{}; cls = @{}; pol = @{} }
function Is-Builtin($x) {
  if ($x.PSObject.Properties['built_in'] -and $x.built_in) { return $true }
  if ($x.PSObject.Properties['is_default_instance'] -and $x.is_default_instance) { return $true }
  return $false
}
function Own-Map($list, [string]$nameField, [string]$idField) {
  $map = @{}
  foreach ($x in $list) {
    if ($Target -eq 'dct' -and (-not $x.dct_managed -or (Is-Builtin $x))) { continue }
    $n = [string]$x.$nameField
    if (-not $map.ContainsKey($n)) { $map[$n] = [string]$x.$idField }
  }
  return $map
}

try {
  if ($Target -eq 'dct') {
    $RemoteAlg = Get-All '/algorithms'
    $RemoteDc = Get-All '/data-classes'
    $RemoteCls = Get-All '/classifiers'
    $RemotePol = Get-All '/discovery-policies'
    $plugins = Get-All '/masking-plugins'
    $core = @($plugins | Where-Object { $_.name -eq 'dlpx-core' -and $_.dct_managed }) + @($plugins | Where-Object { $_.name -eq 'dlpx-core' })
    $CoreId = ''
    if ($core.Count -gt 0) { $CoreId = [string]$core[0].id; foreach ($f in @($core[0].frameworks)) { $Frameworks[[string]$f.name] = [string]$f.id } }
    $Own.alg = Own-Map $RemoteAlg 'name' 'id'
    $Own.dc = Own-Map $RemoteDc 'name' 'id'
    $Own.cls = Own-Map $RemoteCls 'name' 'id'
    $Own.pol = Own-Map $RemotePol 'name' 'id'
  } else {
    $RemoteAlg = Get-All '/algorithms'
    $RemoteDc = Get-All '/domains'
    $RemoteCls = Get-All '/classifiers'
    $RemotePol = Get-All '/profile-sets'
    foreach ($f in @((From-Json (Invoke-Api 'GET' '/algorithm/frameworks?page_size=500')).responseList)) {
      if ($f.PSObject.Properties['plugin'] -and $f.plugin -and $f.plugin.pluginName -eq 'dlpx-core') { $Frameworks[[string]$f.frameworkName] = [string]$f.frameworkId }
    }
    foreach ($f in @((From-Json (Invoke-Api 'GET' '/classifiers/frameworks')).responseList)) { $ClassifierFrameworks[[string]$f.frameworkName] = [string]$f.frameworkId }
    $Own.alg = Own-Map $RemoteAlg 'algorithmName' 'algorithmName'
    $Own.dc = Own-Map $RemoteDc 'domainName' 'domainName'
    $Own.cls = Own-Map $RemoteCls 'classifierName' 'classifierId'
    $Own.pol = Own-Map $RemotePol 'profileSetName' 'profileSetId'
  }
} catch {
  Write-Host (Msg 'connect_fail' $_.Exception.Message); Write-Log "read failed: $($_.Exception.Message)"; exit 1
}

# ── Plugin BR ─────────────────────────────────────────────────────────────────
# Os algoritmos de plugin que o pacote usa (index.plugins: nome no engine -> plugin, nome no DCT).
$PluginAlgs = @()
if ($Index.PSObject.Properties['plugins']) { $PluginAlgs = @($Index.plugins.PSObject.Properties | ForEach-Object { $_.Value | Add-Member -NotePropertyName engineName -NotePropertyValue $_.Name -PassThru }) }

# O algoritmo de plugin no DCT: o do plugin com esse nome (nunca outro de mesmo nome).
function Find-PluginAlg($info) {
  $pl = @($script:DctPlugins | Where-Object { $_.name -eq $info.plugin } | Sort-Object { -[int][bool]$_.dct_managed })
  foreach ($p in $pl) {
    $hit = @($RemoteAlg | Where-Object { $_.PSObject.Properties['plugin_id'] -and [string]$_.plugin_id -eq [string]$p.id -and $_.name -eq $info.name })
    if ($hit.Count -gt 0) { return $hit[0] }
  }
  return $null
}

function Install-Jar([string]$Jar) {
  Write-Host (Msg 'plugin_sending' (Split-Path -Leaf $Jar))
  $boundary = [guid]::NewGuid().ToString('N')
  $head = $Utf8.GetBytes("--$boundary`r`nContent-Disposition: form-data; name=`"file`"; filename=`"$(Split-Path -Leaf $Jar)`"`r`nContent-Type: application/java-archive`r`n`r`n")
  $content = [System.IO.File]::ReadAllBytes($Jar)
  $tail = $Utf8.GetBytes("`r`n--$boundary--`r`n")
  $body = New-Object byte[] ($head.Length + $content.Length + $tail.Length)
  [System.Buffer]::BlockCopy($head, 0, $body, 0, $head.Length)
  [System.Buffer]::BlockCopy($content, 0, $body, $head.Length, $content.Length)
  [System.Buffer]::BlockCopy($tail, 0, $body, $head.Length + $content.Length, $tail.Length)
  $r = Invoke-Api 'POST' '/masking-file-uploads' $body "multipart/form-data; boundary=$boundary"
  if (-not (Test-Ok $r)) { throw (Get-ErrorText $r) }
  $o = From-Json $r
  $ref = ''
  if ($o.PSObject.Properties['file']) { $ref = [string]$o.file.file_reference_id; if (-not $ref) { $ref = [string]$o.file.id } }
  if (-not $ref) { throw 'no file reference' }
  Write-Log "jar uploaded: $ref"
  Write-Host (Msg 'plugin_install')
  $r = Invoke-Api 'POST' '/masking-plugins' (Json-Bytes (New-Object PSObject -Property @{ file_id = $ref; plugin_type = 'EXTENDED_ALGORITHM' }))
  if (-not (Test-Ok $r)) { throw (Get-ErrorText $r) }
  Wait-Done $r
}

if ($PluginAlgs.Count -gt 0) {
  $nomePlugin = [string]$PluginAlgs[0].plugin
  $lista = ($PluginAlgs | ForEach-Object { $_.name }) -join ', '
  if ($Target -eq 'dct') {
    $script:DctPlugins = @($plugins)
    $falta = @($PluginAlgs | Where-Object { -not (Find-PluginAlg $_) })
    if ($falta.Count -gt 0) {
      Write-Host ''
      Write-Host (Msg 'plugin_missing' $nomePlugin $lista) -ForegroundColor Yellow
      Write-Log "plugin missing: $nomePlugin ($lista)"
      $jar = @(Get-ChildItem -LiteralPath $Here -Filter '*.jar' -File | Sort-Object Name -Descending) | Select-Object -First 1
      if (-not $jar) { Write-Host (Msg 'plugin_nojar' $nomePlugin); exit 1 }
      if (-not (Test-Yes (Ask (Msg 'plugin_ask' $jar.Name) 's'))) { Write-Host (Msg 'plugin_nojar' $nomePlugin); Write-Log 'plugin not installed'; exit 1 }
      try { Install-Jar $jar.FullName } catch { Write-Host (Msg 'failed' $_.Exception.Message) -ForegroundColor Red; Write-Log "plugin install failed: $($_.Exception.Message)"; exit 1 }
      $lidos = Get-All '/masking-plugins'
      $script:DctPlugins = @($lidos)
      $RemoteAlg = Get-All '/algorithms'
      $falta = @($PluginAlgs | Where-Object { -not (Find-PluginAlg $_) })
      if ($falta.Count -gt 0) { Write-Host (Msg 'plugin_missing' $nomePlugin (($falta | ForEach-Object { $_.name }) -join ', ')) -ForegroundColor Red; exit 1 }
      Write-Host (Msg 'plugin_done' $nomePlugin) -ForegroundColor Green
      Write-Log "plugin installed: $nomePlugin"
    }
    $p = @($script:DctPlugins | Where-Object { $_.name -eq $nomePlugin } | Sort-Object { -[int][bool]$_.dct_managed })[0]
    $versao = ''; if ($p.PSObject.Properties['version']) { $versao = [string]$p.version }
    Write-Host (Msg 'plugin_ok' $nomePlugin $versao $lista)
  } else {
    $falta = @($PluginAlgs | Where-Object { $n = $_.engineName; -not @($RemoteAlg | Where-Object { $_.algorithmName -eq $n }).Count })
    if ($falta.Count -gt 0) { Write-Host (Msg 'plugin_missing' $nomePlugin $lista) -ForegroundColor Yellow; Write-Host (Msg 'plugin_engine' $nomePlugin); exit 1 }
    Write-Host (Msg 'plugin_ok' $nomePlugin '' $lista)
  }
}

# The existing objects, in the order they are deleted: what uses comes before what is used.
$Existing = New-Object System.Collections.ArrayList
function Add-Existing([string]$Kind, [string]$N) {
  if ($Own[$Kind].ContainsKey($N)) { [void]$Existing.Add((New-Object PSObject -Property @{ Kind = $Kind; Name = $N; Id = $Own[$Kind][$N] })) }
}
Add-Existing 'pol' $PolicyName
foreach ($c in $Classifiers) { Add-Existing 'cls' $c.name }
foreach ($d in $DataClasses) { Add-Existing 'dc' $d.name }
for ($i = $Algorithms.Count - 1; $i -ge 0; $i--) { Add-Existing 'alg' $Algorithms[$i].name }

$Deleted = 0
if ($Existing.Count -eq 0) {
  Write-Host (Msg 'nothing_exists')
} else {
  Write-Host ''
  Write-Host (Msg 'warn_title') -ForegroundColor Yellow
  foreach ($e in $Existing) { Write-Host ("  - " + (Msg "kind_$($e.Kind)") + ": " + $e.Name); Write-Log "exists: $($e.Kind) $($e.Name) ($($e.Id))" }
  Write-Host ''
  Write-Host (Msg 'warn_update')
  if (-not (Test-Yes (Ask (Msg 'confirm') 'n'))) { Write-Host (Msg 'cancelled'); Write-Log 'cancelled'; exit 0 }
  Write-Host ''
  foreach ($e in $Existing) {
    Write-Host ((Msg 'deleting' (Msg "kind_$($e.Kind)") $e.Name) + '… ') -NoNewline
    $id = [System.Uri]::EscapeDataString($e.Id)
    $path = switch ("$Target`:$($e.Kind)") {
      'dct:pol' { "/discovery-policies/$id" } 'dct:cls' { "/classifiers/$id" } 'dct:dc' { "/data-classes/$id" } 'dct:alg' { "/algorithms/$id" }
      'engine:pol' { "/profile-sets/$id" } 'engine:cls' { "/classifiers/$id" } 'engine:dc' { "/domains/$id" } 'engine:alg' { "/algorithms/$id" }
    }
    $r = Invoke-Api 'DELETE' $path
    $reason = ''
    if (Test-Ok $r) { try { Wait-Done $r } catch { $reason = [string]$_.Exception.Message } } else { $reason = Get-ErrorText $r }
    if ($reason) {
      Write-Host (Msg 'kept' $reason); Write-Log "delete refused: $($e.Kind) $($e.Name) — $reason"
      $State.update["$($e.Kind):$($e.Name)"] = $e.Id
    } else {
      Write-Host (Msg 'deleted'); Write-Log "deleted: $($e.Kind) $($e.Name)"; $Deleted++
    }
  }
}

# ── Import ────────────────────────────────────────────────────────────────────

$Created = 0; $Updated = 0
function Stop-Import([string]$Why) {
  Write-Host ''
  Write-Host (Msg 'failed' $Why) -ForegroundColor Red
  Write-Log "FAILED: $Why"
  Write-Host (Msg 'stopped')
  Write-Host (Msg 'log' $LogFile)
  exit 1
}

# Uploads a file. An engine lets every object name one upload; DCT ties an upload to the object that
# named it first, so there each object gets its own.
function Send-File([string]$FileName) {
  if ($Target -eq 'engine' -and $State.files.ContainsKey($FileName)) { return $State.files[$FileName] }
  $boundary = [guid]::NewGuid().ToString('N')
  $head = $Utf8.GetBytes("--$boundary`r`nContent-Disposition: form-data; name=`"file`"; filename=`"$FileName`"`r`nContent-Type: application/octet-stream`r`n`r`n")
  $content = [System.IO.File]::ReadAllBytes((Join-Path (Join-Path $Data 'files') $FileName))
  $tail = $Utf8.GetBytes("`r`n--$boundary--`r`n")
  $body = New-Object byte[] ($head.Length + $content.Length + $tail.Length)
  [System.Buffer]::BlockCopy($head, 0, $body, 0, $head.Length)
  [System.Buffer]::BlockCopy($content, 0, $body, $head.Length, $content.Length)
  [System.Buffer]::BlockCopy($tail, 0, $body, $head.Length + $content.Length, $tail.Length)
  $path = '/file-uploads'; if ($Target -eq 'dct') { $path = '/masking-file-uploads' }
  $r = Invoke-Api 'POST' $path $body "multipart/form-data; boundary=$boundary"
  if (-not (Test-Ok $r)) { throw "${FileName}: $(Get-ErrorText $r)" }
  $o = From-Json $r
  $ref = ''
  if ($o.PSObject.Properties['file']) { $ref = $o.file.file_reference_id } elseif ($o.PSObject.Properties['fileReferenceId']) { $ref = $o.fileReferenceId }
  if (-not $ref) { throw "${FileName}: no file reference" }
  if ($Target -eq 'engine') { $State.files[$FileName] = $ref }
  return $ref
}

# The value a token stands for there.
function Resolve-Token([string]$Kind, [string]$Key) {
  switch ($Kind) {
    'FW' { if ($Frameworks.ContainsKey($Key)) { return $Frameworks[$Key] }; throw (Msg 'no_framework' $Key) }
    'CFW' { if ($ClassifierFrameworks.ContainsKey($Key)) { return $ClassifierFrameworks[$Key] }; throw (Msg 'no_framework' $Key) }
    'FILE' { return Send-File $Key }
    'CLS' { if ($State.cls.ContainsKey($Key)) { return $State.cls[$Key] }; throw "classifier $Key" }
    'DC' {
      if ($State.dc.ContainsKey($Key)) { return $State.dc[$Key] }
      $hit = @($RemoteDc | Where-Object { $_.name -eq $Key -and $_.dct_managed } | Sort-Object { [int][bool](Is-Builtin $_) })
      if ($hit.Count -gt 0) { return [string]$hit[0].id }
      throw "data class $Key"
    }
    'ALG' {
      if ($State.alg.ContainsKey($Key)) { return $State.alg[$Key] }
      if ($Key.StartsWith('dlpx-core:')) {
        if ($Target -eq 'engine') { return $Key }
        $dctName = $Index.builtins.PSObject.Properties[$Key].Value
        $hit = @($RemoteAlg | Where-Object { $_.dct_managed -and $_.is_default_instance -and $_.name -eq $dctName })
        # Dois algoritmos de fábrica com o mesmo nome: vale o do plugin dlpx-core.
        $doCore = @($hit | Where-Object { $CoreId -and $_.PSObject.Properties['plugin_id'] -and [string]$_.plugin_id -eq $CoreId })
        if ($doCore.Count -gt 0) { return [string]$doCore[0].id }
        if ($hit.Count -gt 0) { return [string]$hit[0].id }
        throw (Msg 'no_builtin' $Key)
      }
      if ($Index.PSObject.Properties['plugins'] -and $Index.plugins.PSObject.Properties[$Key]) {
        if ($Target -eq 'engine') { return $Key }
        $a = Find-PluginAlg $Index.plugins.PSObject.Properties[$Key].Value
        if ($a) { return [string]$a.id }
        throw (Msg 'no_builtin' $Key)
      }
      throw "algorithm $Key"
    }
  }
  throw "token $Kind"
}

# A template with every token swapped for its value.
function Render([string]$Template) {
  $text = [System.IO.File]::ReadAllText($Template, $Utf8)
  $tokens = @([regex]::Matches($text, '\{\{[A-Z]+:[^}]*\}\}') | ForEach-Object { $_.Value } | Sort-Object -Unique)
  foreach ($tok in $tokens) {
    $inner = $tok.Substring(2, $tok.Length - 4)
    $colon = $inner.IndexOf(':')
    $value = Resolve-Token $inner.Substring(0, $colon) $inner.Substring($colon + 1)
    $text = $text.Replace($tok, [string]$value)
  }
  return $text
}

# A JSON value written the same way whatever its key order or number spelling, for comparing.
function Normal-Json($value) {
  if ($null -eq $value) { return 'null' }
  if ($value -is [string]) { return ($value | ConvertTo-Json -Compress) }
  if ($value -is [bool]) { return $value.ToString().ToLowerInvariant() }
  if ($value -is [ValueType]) { return ([double]$value).ToString('R', [System.Globalization.CultureInfo]::InvariantCulture) }
  if ($value -is [System.Collections.IEnumerable]) { return '[' + ((@($value) | ForEach-Object { Normal-Json $_ }) -join ',') + ']' }
  $parts = @($value.PSObject.Properties | Sort-Object Name | ForEach-Object { ($_.Name | ConvertTo-Json -Compress) + ':' + (Normal-Json $_.Value) })
  return '{' + ($parts -join ',') + '}'
}

function Select-Fields($json, [string[]]$Keep) {
  $o = $json | ConvertFrom-Json
  $out = New-Object PSObject
  foreach ($k in $Keep) { if ($o.PSObject.Properties[$k]) { $out | Add-Member -NotePropertyName $k -NotePropertyValue $o.$k } }
  return $out
}

# Sends one object: created, or updated when Delphix kept it.
function Send-Object([string]$Kind, [string]$N, [string]$Template, [string]$CreatePath, [string]$UpdatePath) {
  try { $body = Render $Template } catch { Stop-Import "$N — $($_.Exception.Message)" }
  $key = "${Kind}:$N"
  $id = ''
  if ($State.update.ContainsKey($key)) {
    $id = [string]$State.update[$key]
    Write-Host ((Msg 'updating' (Msg "kind_$Kind") $N) + '… ') -NoNewline
    if ($Target -eq 'dct' -and $Kind -eq 'alg') {
      # Already as it would be sent: nothing to write. For a Checkdigit this is also the only way
      # through, since DCT answers 500 to any update of one.
      $had = @($RemoteAlg | Where-Object { [string]$_.id -eq $id })
      $mine = $body | ConvertFrom-Json
      $myDescription = ''; if ($mine.PSObject.Properties['description']) { $myDescription = [string]$mine.description }
      $theirDescription = ''; if ($had.Count -gt 0 -and $had[0].PSObject.Properties['description'] -and $had[0].description) { $theirDescription = [string]$had[0].description }
      if ($had.Count -gt 0 -and (Normal-Json $had[0].config) -eq (Normal-Json $mine.config) -and (-not $myDescription -or $myDescription -eq $theirDescription)) {
        Write-Host (Msg 'unchanged')
        Write-Log "unchanged: $Kind $N ($id)"
        $State.alg[$N] = $id
        return
      }
    }
    if ($Target -eq 'dct') {
      # DCT's updates take only the fields that may change.
      $patch = switch ($Kind) {
        'alg' { Json-Bytes (Select-Fields $body @('config', 'description')) }
        'dc' { Json-Bytes (Select-Fields $body @('default_algorithm_id', 'default_token_algorithm_id', 'description')) }
        'cls' { $Utf8.GetBytes($body) }
        'pol' { Json-Bytes (Select-Fields $body @('name', 'description', 'assignment_threshold')) }
      }
      $r = Invoke-Api 'PATCH' ($UpdatePath + [System.Uri]::EscapeDataString($id)) $patch
    } else {
      $bytes = $Utf8.GetBytes($body)
      if ($Kind -eq 'cls') {
        # An engine keeps a classifier's framework; send the one it has.
        $o = $body | ConvertFrom-Json
        $had = @($RemoteCls | Where-Object { [string]$_.classifierId -eq $id })
        if ($had.Count -gt 0) { $o.frameworkId = $had[0].frameworkId }
        $bytes = Json-Bytes $o
      }
      $r = Invoke-Api 'PUT' ($UpdatePath + [System.Uri]::EscapeDataString($id)) $bytes
    }
    if ($r.Status -eq 500 -and $Target -eq 'dct' -and $Kind -eq 'alg') { Stop-Import (Msg 'checkdigit' $N) }
    if (-not (Test-Ok $r)) { Stop-Import "$N — $(Get-ErrorText $r)" }
    try { Wait-Done $r } catch { Stop-Import "$N — $($_.Exception.Message)" }
    $script:Updated++
    if ($Target -eq 'dct' -and $Kind -eq 'pol') {
      $members = Json-Bytes (Select-Fields $body @('classifier_ids'))
      $m = Invoke-Api 'POST' "/discovery-policies/$id/classifiers" $members
      if (Test-Ok $m) { try { Wait-Done $m } catch { Write-Log "members not added: $($_.Exception.Message)" } }
    }
  } else {
    Write-Host ((Msg 'creating' (Msg "kind_$Kind") $N) + '… ') -NoNewline
    $r = Invoke-Api 'POST' $CreatePath $Utf8.GetBytes($body)
    if (-not (Test-Ok $r)) { Stop-Import "$N — $(Get-ErrorText $r)" }
    try { Wait-Done $r } catch { Stop-Import "$N — $($_.Exception.Message)" }
    $script:Created++
    $o = From-Json $r
    if ($Target -eq 'dct') { $id = [string]$o.id }
    elseif ($Kind -eq 'alg' -or $Kind -eq 'dc') { $id = $N }
    elseif ($Kind -eq 'cls') { $id = [string]$o.classifierId }
    else { $id = [string]$o.profileSetId }
  }
  Write-Host (Msg 'ok')
  Write-Log "sent: $Kind $N ($id)"
  if ($Kind -eq 'alg') { $State.alg[$N] = $id } elseif ($Kind -eq 'dc') { $State.dc[$N] = $id } elseif ($Kind -eq 'cls') { $State.cls[$N] = $id }
}

Write-Host ''
if ($Target -eq 'dct') {
  $Tpl = Join-Path $Data 'dct'
  $Paths = @{ alg = @('/algorithms', '/algorithms/'); dc = @('/data-classes', '/data-classes/'); cls = @('/classifiers', '/classifiers/'); pol = @('/discovery-policies', '/discovery-policies/') }
} else {
  $Tpl = Join-Path $Data 'engine'
  $Paths = @{ alg = @('/algorithms', '/algorithms/'); dc = @('/domains', '/domains/'); cls = @('/classifiers', '/classifiers/'); pol = @('/profile-sets', '/profile-sets/') }
}
function Template-Path([string]$Relative) { return Join-Path $Tpl ($Relative -replace '/', [System.IO.Path]::DirectorySeparatorChar) }

foreach ($a in $Algorithms) { Send-Object 'alg' $a.name (Template-Path $a.file) $Paths.alg[0] $Paths.alg[1] }
foreach ($d in $DataClasses) { Send-Object 'dc' $d.name (Template-Path $d.file) $Paths.dc[0] $Paths.dc[1] }
foreach ($c in $Classifiers) { Send-Object 'cls' $c.name (Template-Path $c.file) $Paths.cls[0] $Paths.cls[1] }
Send-Object 'pol' $PolicyName (Template-Path $PolicyFile) $Paths.pol[0] $Paths.pol[1]

Write-Host ''
Write-Host (Msg 'done' $Created $Updated $Deleted) -ForegroundColor Green
Write-Host (Msg 'log' $LogFile)
Write-Log "done: $Created created, $Updated updated, $Deleted deleted"
