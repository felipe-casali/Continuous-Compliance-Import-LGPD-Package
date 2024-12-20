import csv
import json

def csv_to_json(csv_filepath, json_filepath):
    data = []
    with open(csv_filepath, 'r', encoding='utf-8') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            try:
                config_type = row['configType']
                classifier_configuration = {
                    config_type: row['configDetails'],
                    "rejectStrength": float(row['rejectStrength'])
                }

                row_data = {
                    "domainName": row["domainName"],
                    "classifierName": row["classifierName"],
                    "description": row["description"],
                    "frameworkId": int(row["frameworkId"]),
                    "classifierConfiguration": classifier_configuration
                }
                data.append(row_data)

            except ValueError as e:
                print(f"Erro ao converter para int/float na linha: {row}")
                print(f"Erro: {e}")
                continue

    with open(json_filepath, 'w', encoding='utf-8') as jsonfile:
        json.dump(data, jsonfile, indent=4, ensure_ascii=False)

csv_to_json('./CSV/classifiers.csv', './JSON/classifiers.json')