# Pharma-IMS: Application Functionalities

Pharmaceutical Inventory Management System (Pharma-IMS) is a comprehensive solution designed to handle both retail pharmacy inventory and pharmaceutical manufacturing ERP (Enterprise Resource Planning) workflows.

---

## 1. Material Master Management (Drug Master)
The core of the system is the **Drug Master**, which classifies all items handled by the facility:
- **Material Classification**: Categorizes items as Raw Materials (APIs), Excipients, Packaging, or Finished Goods.
- **Unit of Measure (UOM)**: Supports various units like kilograms (kg), milliliters (ml), grams (g), and individual units (Unit).
- **Technical Specs**: Tracks strength, formulation (tablet, syrup, etc.), schedule category (OTC, Schedule H), and storage conditions.
- **Supplier Mapping**: Links materials to preferred suppliers for automated procurement guidance.

## 2. Inventory & Warehouse Control
A robust tracking system for physical stock across multiple internal locations:
- **Location Tracking**: Manages stocks in different zones such as Main Warehouse, Cold Storage, and Production Floors.
- **Batch Management**: Every item is tracked by a unique Batch Number, essential for pharma compliance.
- **Expiry Monitoring**: Automatic tracking of expiry dates for all stock batches.
- **Stock Movements**: Real-time updates of inventory levels based on procurement and production consumption.

## 3. Manufacturing ERP (Production)
The production module manages the transformation of raw chemicals into final drug products:
- **Bill of Materials (BOM)**: Defines the "recipe" for finished drugs, specifying exact quantities of multiple ingredients (APIs/Excipients) and packaging required.
- **Production Orders**: Manages the manufacturing lifecycle:
    - **Planned**: Scheduled production runs.
    - **In-Production**: Actively being manufactured.
    - **Quality-Testing**: Awaiting lab results.
    - **Released/Rejected**: Final outcome of the manufacturing batch.
- **Traceability**: Links finished product batches back to the raw material batches used to create them.

## 4. Quality Control & Assurance (QC/QA)
Ensures that all pharmaceutical products meet safety standards before use or sale:
- **QC Status Workflow**: Items newly received or produced are placed in **Quarantine**.
- **Analysis & Release**: QA Analysts can update batch status to **Released** after testing.
- **Rejection Logic**: Defective batches are moved to **Rejected** status and isolated in inventory.

## 5. Supply Chain & Procurement
Handles the interaction with external vendors:
- **Supplier Master**: Detailed database of vendors, including their GSTIN and Drug License (DL) details.
- **Purchase Orders (PO)**: Digital requisition of materials from suppliers.
- **Goods Received Note (GRN)**: The intake process where physical arrivals are verified against POs and converted into inventory batches.

## 6. Security & Compliance (Audit Trail)
Designed for highly regulated pharmaceutical environments:
- **Role-Based Access (RBAC)**: Specific interfaces for Admins, Production Managers, and QA Analysts.
- **System Audit Trail**: An immutable log that records every critical action:
    - Who updated a QC status?
    - When was a stock adjustment made?
    - Which user created a production order?
- **Immunity to Deletion**: Strategic database design prevents accidental data loss of critical compliance records.

---

*This document serves as the official functional specification for the Pharma-IMS platform.*
