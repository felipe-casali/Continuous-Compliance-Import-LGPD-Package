# Masking Algorithm SDK Version Info

## Version

This is version 1.11.0 of the Delphix Masking Algorithm SDK.

## Change Log

### Version 1.11.0
 * Updated the Masking Extensibility API version to 1.11.0, which includes
   * new classes to support future sensitive data discovery features
   * fixes for error handling and logging
 * Updated the Delphix core algorithm plugin version to 1.11.0, which includes
   * fixes for Segment Mapping, Date Shift, and Name algorithms

### Version 1.10.0
 * Updated the Masking Extensibility API version to 1.10.0
 * Includes the 1.10.0 version of the Delphix core algorithm plugin.

### Version 1.9.0
 * Updated the Masking Extensibility API version to 1.9.0
 * Includes the 1.9.0 version of the Delphix core algorithm plugin.
 * Added support for optional fields in multi-column algorithms, including -M option for
   specifying the logical fields when masking.
 * Added the MultiColumnRedaction example algorithm

### Version 1.8.0
 * Updated the extensibility API and Core Algorithm plugin to the latest version

### Version 1.6.0
 * Added -m option to maskApp mask subcommand to support testing of tokenization algorithms
 * Granted plugins the permission to delete files through the maskApp and maskScript.

### Version 1.5.0
 * Introduced driverSupport sample project
 * Introduced taskExecute maskScript command to test driverSupport tasks
 * Introduced support for batch mode masking (mask -b argument)
 * Fixed a bug that would cause maskScript to fail with RuntimeException

### Version 1.4.0
 * Upgraded Masking Algorithm API version to 1.4.0
 * The dlpx-core algorithm plugin, containing the real algorithm implementations included with the Masking Engine,
   is now available for testing in the SDK.

### Version 1.3.0
 * Upgraded Masking Algorithm API version to 1.3.0
 * Upgraded Jackson version to 2.11.2
 * Type conversion implementations are now part of the Masking Algorithm API, so conversions will be consistent with
   those performed by masking jobs run on the Delphix Engine

### Version 1.2.0
 * Upgraded Masking Algorithm API version to 1.2.0
 * Added support for multi-column algorithms

### Version 1.1.0
 * Upgraded Masking Algorithm API version to 1.1.0
 * Added support for JDBC connection references 

### Version 1.0.0

Initial Version, shipped with Masking Algorithm API version 1.0.0
