# 01_export_mysql.ps1
# Description: Dumps legacy MySQL tables to CSV format.

$dbUser = "root"
$dbPass = "password" # UPDATE BEFORE RUNNING
$dbName = "pharma_scm"
$outputDir = ".\export"

If (!(Test-Path $outputDir)) {
    New-Item -ItemType Directory -Force -Path $outputDir
}

# Tables in FK-safe export order
$tables = @(
    "Role_Master",
    "Permission_Master",
    "Role_Permission",
    "User_Master",
    "System_Audit_Trail",
    "Location_Master",
    "Supplier_Master",
    "Supplier_Audit_Log",
    "Material_Master",
    "Purchase_Order",
    "PurchaseOrder_Item",
    "Goods_Received_Note",
    "GRN_Item",
    "BOM_Header",
    "BOM_Details",
    "Production_Order",
    "Production_Batch",
    "Stock_Inventory",
    "Inventory_Transaction",
    "Production_Material_Consumption",
    "Batch_Genealogy",
    "Event_Log",
    "QA_Records",
    "Deviation_Records",
    "CAPA_Records",
    "Compliance_Records"
)

foreach ($table in $tables) {
    Write-Host "Exporting $table..."
    $query = "SELECT * INTO OUTFILE '$((Get-Location).Path)\export\$table.csv' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' LINES TERMINATED BY '\n' FROM $table;"
    # Execute query using mysql CLI. In a real environment, secure the password.
    mysql -u $dbUser -p$dbPass -D $dbName -e $query
}

Write-Host "Export complete. CSV files are located in $outputDir"
