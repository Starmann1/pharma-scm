import './style.css';

// REST API Configuration
const API_BASE = "http://localhost:8080/api";

// Local State Store
let currentView = 'materials';
let stockSubTab = 'current'; // 'current' or 'transactions'
let prodSubTab = 'runs'; // 'runs' or 'bom'

let materials = [];
let suppliers = [];
let stockList = [];
let txList = [];
let bomList = [];
let orderList = [];
let qaList = [];

let editTargetMaterialCode = null;
let editTargetSupplierId = null;

// Persistent DOM Nodes
const mainViewport = document.getElementById('mainViewport');
const breadcrumbCurrent = document.getElementById('breadcrumbCurrent');
const searchInput = document.getElementById('globalSearch');

// Dialog Nodes
const addMaterialForm = document.getElementById('addMaterialForm');
const editMaterialForm = document.getElementById('editMaterialForm');
const addSupplierForm = document.getElementById('addSupplierForm');
const editSupplierForm = document.getElementById('editSupplierForm');
const supplierStatusForm = document.getElementById('supplierStatusForm');
const addProductionOrderForm = document.getElementById('addProductionOrderForm');
const addBomForm = document.getElementById('addBomForm');
const inspectBatchForm = document.getElementById('inspectBatchForm');

// XAI Nodes
const xaiTrigger = document.getElementById('xaiTrigger');
const xaiPanel = document.getElementById('xaiPanel');
const xaiClose = document.getElementById('xaiClose');
const xaiPulse = document.getElementById('xaiPulse');
const tabLogs = document.getElementById('tabLogs');
const tabChat = document.getElementById('tabChat');
const viewLogs = document.getElementById('viewLogs');
const viewChat = document.getElementById('viewChat');
const chatInput = document.getElementById('chatInput');
const chatList = document.getElementById('chatList');
const sendChatBtn = document.getElementById('sendChatBtn');

/* --- ROUTER SETUP --- */
function initRouter() {
    const menuItems = [
        { id: 'menu-overview', view: 'overview', label: 'Overview Dashboard' },
        { id: 'menu-materials', view: 'materials', label: 'Materials Master' },
        { id: 'menu-inventory', view: 'inventory', label: 'Stock & Inventory' },
        { id: 'menu-suppliers', view: 'suppliers', label: 'Suppliers Registry' },
        { id: 'menu-production', view: 'production', label: 'Production Runs & BOM' },
        { id: 'menu-compliance', view: 'compliance', label: 'QA Compliance Panel' },
        { id: 'menu-admin', view: 'admin', label: 'Admin Management' }
    ];

    menuItems.forEach(item => {
        const btn = document.getElementById(item.id);
        if (btn) {
            btn.addEventListener('click', () => {
                menuItems.forEach(i => document.getElementById(i.id).classList.remove('active'));
                btn.classList.add('active');
                
                currentView = item.view;
                breadcrumbCurrent.textContent = item.label;
                searchInput.value = ''; // Reset search
                
                loadViewData(item.view);
            });
        }
    });

    const sidebarToggle = document.getElementById('sidebarToggle');
    const sidebar = document.getElementById('appSidebar');
    sidebarToggle.addEventListener('click', () => {
        sidebar.classList.toggle('collapsed');
        sidebarToggle.textContent = sidebar.classList.contains('collapsed') ? '▶' : '◀';
        sidebarToggle.title = sidebar.classList.contains('collapsed') ? 'Expand Menu' : 'Collapse Menu';
    });
}

// Load data corresponding to active view
async function loadViewData(view) {
    if (view === 'materials') {
        await fetchMaterials();
    } else if (view === 'suppliers') {
        await fetchSuppliers();
    } else if (view === 'inventory') {
        await fetchStockData();
    } else if (view === 'production') {
        await fetchProductionData();
    } else if (view === 'compliance') {
        await fetchComplianceData();
    } else if (view === 'overview') {
        await fetchOverviewData();
    } else {
        renderPlaceholderView(view);
    }
}

/* --- HTTP API CALLS --- */

// 1. Materials REST Callers
async function fetchMaterials() {
    try {
        const res = await fetch(`${API_BASE}/materials`);
        if (!res.ok) throw new Error("Failed to load materials");
        materials = await res.json();
        renderMaterialsView(materials);
    } catch (err) {
        console.error("API error: ", err);
        showMockMaterialsFallback();
    }
}

async function createMaterial(item) {
    try {
        const res = await fetch(`${API_BASE}/materials`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(item)
        });
        if (!res.ok) throw new Error("Failed to create material");
        closeModal('addMaterialModal');
        addMaterialForm.reset();
        await fetchMaterials();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function updateMaterial(code, item) {
    try {
        const res = await fetch(`${API_BASE}/materials/${code}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(item)
        });
        if (!res.ok) throw new Error("Failed to update material");
        closeModal('editMaterialModal');
        await fetchMaterials();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function deleteMaterial(code) {
    if (!confirm(`Are you sure you want to delete material ${code}?`)) return;
    try {
        const res = await fetch(`${API_BASE}/materials/${code}`, { method: 'DELETE' });
        if (!res.ok) throw new Error("Failed to delete material");
        await fetchMaterials();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// 2. Suppliers REST Callers
async function fetchSuppliers() {
    try {
        const res = await fetch(`${API_BASE}/suppliers`);
        if (!res.ok) throw new Error("Failed to load suppliers");
        suppliers = await res.json();
        renderSuppliersView(suppliers);
    } catch (err) {
        console.error("API error: ", err);
        showMockSuppliersFallback();
    }
}

async function createSupplier(item) {
    try {
        const res = await fetch(`${API_BASE}/suppliers`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(item)
        });
        if (!res.ok) throw new Error("Failed to create supplier");
        closeModal('addSupplierModal');
        addSupplierForm.reset();
        await fetchSuppliers();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function updateSupplier(id, item) {
    try {
        const res = await fetch(`${API_BASE}/suppliers/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(item)
        });
        if (!res.ok) throw new Error("Failed to update supplier");
        closeModal('editSupplierModal');
        await fetchSuppliers();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function deleteSupplier(id) {
    if (!confirm(`Are you sure you want to delete supplier record ID: ${id}?`)) return;
    try {
        const res = await fetch(`${API_BASE}/suppliers/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error("Failed to delete supplier");
        await fetchSuppliers();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function patchSupplierStatus(id, status, remarks, performedBy) {
    try {
        const res = await fetch(`${API_BASE}/suppliers/${id}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status, remarks, performedBy })
        });
        if (!res.ok) throw new Error("Failed to update supplier status");
        closeModal('supplierStatusModal');
        supplierStatusForm.reset();
        await fetchSuppliers();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// 3. Stock / Inventory REST Callers
async function fetchStockData() {
    try {
        const resStock = await fetch(`${API_BASE}/stock`);
        if (!resStock.ok) throw new Error("Failed to load stock");
        stockList = await resStock.json();

        const resTx = await fetch(`${API_BASE}/stock/transactions`);
        if (resTx.ok) {
            txList = await resTx.json();
        }
        renderStockView();
    } catch (err) {
        console.error("API error: ", err);
        showMockStockFallback();
    }
}

// 4. Production & BOM REST Callers
async function fetchProductionData() {
    try {
        const resBoms = await fetch(`${API_BASE}/bom`);
        if (resBoms.ok) bomList = await resBoms.json();

        const resOrders = await fetch(`${API_BASE}/production/orders`);
        if (resOrders.ok) orderList = await resOrders.json();

        renderProductionView();
    } catch (err) {
        console.error("API error fetching production: ", err);
        showMockProductionFallback();
    }
}

async function createBom(payload) {
    try {
        const res = await fetch(`${API_BASE}/bom`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error("Failed to save BOM recipe");
        closeModal('addBomModal');
        addBomForm.reset();
        await fetchProductionData();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function createProductionOrder(payload) {
    try {
        const res = await fetch(`${API_BASE}/production/orders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error("Failed to schedule production order");
        closeModal('addProductionOrderModal');
        addProductionOrderForm.reset();
        await fetchProductionData();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function startProductionOrder(orderId) {
    try {
        const res = await fetch(`${API_BASE}/production/orders/${orderId}/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: 1 })
        });
        if (!res.ok) {
            const errObj = await res.json().catch(() => ({}));
            throw new Error(errObj.error || "Material shortage or process run failed");
        }
        await fetchProductionData();
    } catch (err) {
        alert("Execution Error: " + err.message);
    }
}

async function sendToQATesting(orderId) {
    try {
        const res = await fetch(`${API_BASE}/production/orders/${orderId}/status`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: "Quality-Testing" })
        });
        if (!res.ok) throw new Error("Failed to submit to QA department");
        await fetchProductionData();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// 5. QA Compliance REST Callers
async function fetchComplianceData() {
    try {
        const res = await fetch(`${API_BASE}/qa/inspections`);
        if (!res.ok) throw new Error("Failed to load QA inspections");
        qaList = await res.json();
        renderComplianceView(qaList);
    } catch (err) {
        console.error("API error fetching QA list: ", err);
        showMockQAFallback();
    }
}

async function submitQADecision(batch, status, remarks, performedBy) {
    try {
        const res = await fetch(`${API_BASE}/qa/inspections/${batch}/inspect`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status, remarks, performedBy })
        });
        if (!res.ok) {
            const errObj = await res.json().catch(() => ({}));
            throw new Error(errObj.error || "Failed to update batch status");
        }
        closeModal('inspectBatchModal');
        inspectBatchForm.reset();
        await fetchComplianceData();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function fetchOverviewData() {
    try {
        const [rMat, rSup, rStk, rQa] = await Promise.all([
            fetch(`${API_BASE}/materials`),
            fetch(`${API_BASE}/suppliers`),
            fetch(`${API_BASE}/stock`),
            fetch(`${API_BASE}/qa/inspections`)
        ]);
        materials = rMat.ok ? await rMat.json() : [];
        suppliers = rSup.ok ? await rSup.json() : [];
        stockList = rStk.ok ? await rStk.json() : [];
        qaList = rQa.ok ? await rQa.json() : [];
        renderOverviewView();
    } catch (err) {
        showMockOverviewStats();
    }
}

/* --- FALLBACK DATA WRAPPERS --- */
function showMockMaterialsFallback() {
    materials = [
        { materialCode: "MAT-001", brandName: "Amoxicillin 500mg", genericName: "Amoxicillin Trihydrate", formulation: "Capsule", strength: "500 mg", materialType: "FINISHED_GOOD", unitOfMeasure: "BOX", active: true },
        { materialCode: "MAT-002", brandName: "Paracetamol API", genericName: "Paracetamol USP", formulation: "Powder", strength: "100% Pure", materialType: "RAW_MATERIAL", unitOfMeasure: "KILOGRAM", active: true },
        { materialCode: "MAT-003", brandName: "Glycerol Excipient", genericName: "Glycerine BP", formulation: "Liquid", strength: "99.5%", materialType: "EXCIPIENT", unitOfMeasure: "LITRE", active: true },
        { materialCode: "MAT-004", brandName: "PVC Blister Foil", genericName: "Polyvinyl Chloride", formulation: "Roll", strength: "250 micron", materialType: "PACKAGING", unitOfMeasure: "ROLL", active: false }
    ];
    renderMaterialsView(materials);
}

function showMockSuppliersFallback() {
    suppliers = [
        { supplierId: 1, supplierName: "Bayer Chemicals Ltd", contactPerson: "Sarah Connor", phone: "+49 30 123456", email: "sarah.connor@bayer.de", gstin: "27DEAAAB8381C", drugLicenseNo: "DL/B-194", paymentTerms: "Net 45", supplierStatus: "APPROVED" },
        { supplierId: 2, supplierName: "Global API Dist", contactPerson: "John Wick", phone: "+1 555 8989", email: "j.wick@continental.com", gstin: "27USAAA9090D", drugLicenseNo: "DL/G-99", paymentTerms: "Net 30", supplierStatus: "PENDING" }
    ];
    renderSuppliersView(suppliers);
}

function showMockStockFallback() {
    stockList = [
        { stockId: 101, materialCode: "MAT-002", locationCode: "RAW_STORES_A", batchNumber: "B-PCM-901", quantity: 1200.0, reservedQuantity: 200.0, availableQuantity: 1000.0, unitCost: 15.5, expDate: "2028-06-30", qcStatus: "APPROVED" },
        { stockId: 102, materialCode: "MAT-003", locationCode: "LIQUID_BAY_1", batchNumber: "B-GLY-22", quantity: 450.0, reservedQuantity: 0.0, availableQuantity: 450.0, unitCost: 8.2, expDate: "2027-12-15", qcStatus: "APPROVED" }
    ];
    txList = [
        { transactionId: 1001, materialCode: "MAT-002", batchNumber: "B-PCM-901", locationCode: "RAW_STORES_A", transactionType: "GOODS_RECEIPT", quantity: 1200.0, notes: "Mocked receipt" }
    ];
    renderStockView();
}

function showMockProductionFallback() {
    bomList = [
        { bomId: 1, materialCode: "MAT-001", versionNumber: 1, description: "Amoxicillin recipe v1", isActive: true }
    ];
    orderList = [
        { orderId: 201, batchNumber: "B-AMX-500", bomId: 1, plannedQty: 1000.0, actualQty: 1000.0, status: "IN_PRODUCTION", productionDate: "2026-07-09", notes: "First pilot test" }
    ];
    renderProductionView();
}

function showMockQAFallback() {
    qaList = [
        { stockId: 103, materialCode: "MAT-001", locationCode: "QC_HOLD", batchNumber: "B-AMX-08", quantity: 80.0, reservedQuantity: 0.0, availableQuantity: 0.0, unitCost: 45.0, expDate: "2026-08-01", qcStatus: "UNDER_TEST" }
    ];
    renderComplianceView(qaList);
}

function showMockOverviewStats() {
    materials = [{ active: true }, { active: true }];
    suppliers = [{ supplierStatus: 'APPROVED' }];
    stockList = [{ qcStatus: 'APPROVED' }];
    qaList = [{ qcStatus: 'UNDER_TEST' }];
    renderOverviewView();
}

/* --- VIEW RENDERERS --- */

// 1. Overview Dashboard View
function renderOverviewView() {
    const totalMaterials = materials.length;
    const activeMaterials = materials.filter(m => m.active).length;
    const approvedSuppliers = suppliers.filter(s => s.supplierStatus === 'APPROVED').length;
    const pendingSuppliers = suppliers.filter(s => s.supplierStatus === 'PENDING').length;
    const underTestBatches = qaList.length;

    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">SCM Operations Control Board</h1>
        </div>
        
        <div class="kpi-grid">
            <div class="kpi-card teal">
                <span class="kpi-title">Active Materials</span>
                <span class="kpi-value">${activeMaterials} / ${totalMaterials}</span>
                <span class="kpi-desc">Drugs and excipients available for manufacture</span>
            </div>
            <div class="kpi-card green">
                <span class="kpi-title">Qualified Vendors</span>
                <span class="kpi-value">${approvedSuppliers}</span>
                <span class="kpi-desc">${pendingSuppliers} pending compliance audit checks</span>
            </div>
            <div class="kpi-card amber">
                <span class="kpi-title">QA Inspections</span>
                <span class="kpi-value">${underTestBatches}</span>
                <span class="kpi-desc">Quarantined batches pending release</span>
            </div>
        </div>

        <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-top: 10px;">
            <div class="card-container" style="padding: 20px;">
                <h2 style="font-size: 13px; font-weight:600; margin-bottom: 15px; color: var(--text-primary);">Postgres SCM Decoupling Architecture Status</h2>
                <div style="display:flex; flex-direction:column; gap:10px; line-height: 1.5; color: var(--text-secondary);">
                    <div>✅ <strong>Database Repository Split:</strong> 100% split. All inline SQL logic removed.</div>
                    <div>✅ <strong>REST Layer cutover:</strong> Active Javalin API server running on port 8080.</div>
                    <div>✅ <strong>Unified UI Portal:</strong> Vite Single Page App running locally.</div>
                    <div>✅ <strong>BOM & Production Runs:</strong> Dynamic execution runs with parent-stock allocation added.</div>
                    <div>✅ <strong>Quarantine & Compliance:</strong> QA Inspections workflow integrated with location cutovers.</div>
                </div>
            </div>
            <div class="card-container" style="padding: 20px;">
                <h2 style="font-size: 13px; font-weight:600; margin-bottom: 15px; color: var(--text-primary);">Agent Integration</h2>
                <div style="display:flex; flex-direction:column; gap:8px;">
                    <div style="font-size: 11px;">JADE Platform: <span class="badge badge-success">Online</span></div>
                    <div style="font-size: 11px;">Explainable Overlay: <span class="badge badge-warning">Monitoring</span></div>
                </div>
            </div>
        </div>
    `;
}

// 2. Materials Catalog View
function renderMaterialsView(data) {
    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">Active Materials Master</h1>
            <button class="btn-primary" id="openAddMaterialBtn">+ Add New Material</button>
        </div>

        <div class="card-container">
            <div class="table-responsive">
                <table>
                    <thead>
                        <tr>
                            <th>Code</th>
                            <th>Brand Name</th>
                            <th>Generic Name</th>
                            <th>Formulation</th>
                            <th>Strength</th>
                            <th>Type</th>
                            <th>UoM</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="materialsTableBody"></tbody>
                </table>
            </div>
        </div>
    `;

    const bodyNode = document.getElementById('materialsTableBody');
    bodyNode.innerHTML = '';
    
    if (data.length === 0) {
        bodyNode.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-secondary);">No materials matched this query</td></tr>`;
    } else {
        data.forEach(item => {
            const tr = document.createElement('tr');
            let typeBadge = 'badge-success';
            if (item.materialType === 'PACKAGING') typeBadge = 'badge-warning';
            
            const activeBadge = item.active 
                ? `<span class="badge badge-success">Active</span>`
                : `<span class="badge badge-danger">Inactive</span>`;

            tr.innerHTML = `
                <td>${item.materialCode}</td>
                <td><strong>${item.brandName}</strong></td>
                <td>${item.genericName || '-'}</td>
                <td>${item.formulation || '-'}</td>
                <td>${item.strength || '-'}</td>
                <td><span class="badge ${typeBadge}">${item.materialType}</span></td>
                <td>${item.unitOfMeasure}</td>
                <td>${activeBadge}</td>
                <td>
                    <div class="action-group">
                        <button class="action-btn edit-material-btn" data-code="${item.materialCode}">✏️</button>
                        <button class="action-btn delete-material-btn" data-code="${item.materialCode}">🗑️</button>
                    </div>
                </td>
            `;
            bodyNode.appendChild(tr);
        });
    }

    document.getElementById('openAddMaterialBtn').addEventListener('click', () => openModal('addMaterialModal'));
    document.querySelectorAll('.edit-material-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const code = e.currentTarget.getAttribute('data-code');
            launchEditMaterialModal(code);
        });
    });
    document.querySelectorAll('.delete-material-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const code = e.currentTarget.getAttribute('data-code');
            deleteMaterial(code);
        });
    });
}

// 3. Suppliers Registry View
function renderSuppliersView(data) {
    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">Suppliers & Vendor Master</h1>
            <button class="btn-primary" id="openAddSupplierBtn">+ Add New Supplier</button>
        </div>

        <div class="card-container">
            <div class="table-responsive">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Supplier Name</th>
                            <th>Contact Person</th>
                            <th>Phone</th>
                            <th>GSTIN</th>
                            <th>License No</th>
                            <th>Pay Terms</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="suppliersTableBody"></tbody>
                </table>
            </div>
        </div>
    `;

    const bodyNode = document.getElementById('suppliersTableBody');
    bodyNode.innerHTML = '';

    if (data.length === 0) {
        bodyNode.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-secondary);">No suppliers matched this query</td></tr>`;
    } else {
        data.forEach(item => {
            const tr = document.createElement('tr');
            let statusBadge = 'badge-warning';
            if (item.supplierStatus === 'APPROVED') statusBadge = 'badge-success';
            if (item.supplierStatus === 'REJECTED' || item.supplierStatus === 'SUSPENDED') statusBadge = 'badge-danger';

            tr.innerHTML = `
                <td>${item.supplierId}</td>
                <td><strong>${item.supplierName}</strong></td>
                <td>${item.contactPerson || '-'}</td>
                <td>${item.phone || '-'}</td>
                <td>${item.gstin}</td>
                <td>${item.drugLicenseNo}</td>
                <td>${item.paymentTerms || '-'}</td>
                <td><span class="badge ${statusBadge}">${item.supplierStatus}</span></td>
                <td>
                    <div class="action-group">
                        <button class="action-btn edit-supplier-btn" data-id="${item.supplierId}">✏️</button>
                        <button class="action-btn approve-supplier-btn" data-id="${item.supplierId}">✅</button>
                        <button class="action-btn reject-supplier-btn" data-id="${item.supplierId}">❌</button>
                        <button class="action-btn delete-supplier-btn" data-id="${item.supplierId}">🗑️</button>
                    </div>
                </td>
            `;
            bodyNode.appendChild(tr);
        });
    }

    document.getElementById('openAddSupplierBtn').addEventListener('click', () => openModal('addSupplierModal'));
    document.querySelectorAll('.edit-supplier-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = parseInt(e.currentTarget.getAttribute('data-id'));
            launchEditSupplierModal(id);
        });
    });
    document.querySelectorAll('.delete-supplier-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = parseInt(e.currentTarget.getAttribute('data-id'));
            deleteSupplier(id);
        });
    });
    document.querySelectorAll('.approve-supplier-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = parseInt(e.currentTarget.getAttribute('data-id'));
            launchStatusModal(id, 'APPROVED');
        });
    });
    document.querySelectorAll('.reject-supplier-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = parseInt(e.currentTarget.getAttribute('data-id'));
            launchStatusModal(id, 'REJECTED');
        });
    });
}

// 4. Stock & Inventory View
function renderStockView() {
    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">Warehouse Inventory Management</h1>
        </div>

        <div class="view-tabs-header">
            <button class="tab-btn ${stockSubTab === 'current' ? 'active' : ''}" id="stockSubTabCurrent">Current Stock Batches</button>
            <button class="tab-btn ${stockSubTab === 'transactions' ? 'active' : ''}" id="stockSubTabTx">Transaction History log</button>
        </div>

        <div class="card-container">
            <div class="table-responsive" id="stockViewContent"></div>
        </div>
    `;

    document.getElementById('stockSubTabCurrent').addEventListener('click', () => {
        stockSubTab = 'current';
        renderStockView();
    });
    document.getElementById('stockSubTabTx').addEventListener('click', () => {
        stockSubTab = 'transactions';
        renderStockView();
    });

    const contentDiv = document.getElementById('stockViewContent');
    
    if (stockSubTab === 'current') {
        contentDiv.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>Batch Number</th>
                        <th>Material Code</th>
                        <th>Location</th>
                        <th>Total Qty</th>
                        <th>Reserved</th>
                        <th>Available</th>
                        <th>Unit Cost</th>
                        <th>Expiry Date</th>
                        <th>QC Status</th>
                    </tr>
                </thead>
                <tbody id="stockTableBody"></tbody>
            </table>
        `;
        const bodyNode = document.getElementById('stockTableBody');
        if (stockList.length === 0) {
            bodyNode.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-secondary);">No active stock found</td></tr>`;
        } else {
            stockList.forEach(item => {
                const tr = document.createElement('tr');
                let qcBadge = 'badge-warning';
                if (item.qcStatus === 'APPROVED' || item.qcStatus === 'RELEASED') qcBadge = 'badge-success';
                if (item.qcStatus === 'REJECTED') qcBadge = 'badge-danger';

                tr.innerHTML = `
                    <td><strong>${item.batchNumber}</strong></td>
                    <td>${item.materialCode}</td>
                    <td><code>${item.locationCode}</code></td>
                    <td>${item.quantity}</td>
                    <td>${item.reservedQuantity}</td>
                    <td>${item.availableQuantity}</td>
                    <td>$${item.unitCost.toFixed(2)}</td>
                    <td>${item.expDate || '-'}</td>
                    <td><span class="badge ${qcBadge}">${item.qcStatus}</span></td>
                `;
                bodyNode.appendChild(tr);
            });
        }
    } else {
        contentDiv.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>Tx ID</th>
                        <th>Material</th>
                        <th>Batch</th>
                        <th>Location</th>
                        <th>Type</th>
                        <th>Quantity</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody id="txTableBody"></tbody>
            </table>
        `;
        const bodyNode = document.getElementById('txTableBody');
        if (txList.length === 0) {
            bodyNode.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-secondary);">No inventory transactions recorded</td></tr>`;
        } else {
            txList.forEach(item => {
                const tr = document.createElement('tr');
                let typeBadge = 'badge-success';
                if (item.transactionType.includes('CONSUMPTION') || item.transactionType.includes('ISSUE')) typeBadge = 'badge-danger';

                tr.innerHTML = `
                    <td>${item.transactionId}</td>
                    <td>${item.materialCode}</td>
                    <td><strong>${item.batchNumber}</strong></td>
                    <td><code>${item.locationCode || 'STOCK'}</code></td>
                    <td><span class="badge ${typeBadge}">${item.transactionType}</span></td>
                    <td>${item.quantity}</td>
                    <td>${item.notes || '-'}</td>
                `;
                bodyNode.appendChild(tr);
            });
        }
    }
}

// 5. Production Runs & BOM View
function renderProductionView() {
    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">Production Execution Control</h1>
        </div>

        <div class="view-tabs-header">
            <button class="tab-btn ${prodSubTab === 'runs' ? 'active' : ''}" id="prodSubTabRuns">Production Run Batches</button>
            <button class="tab-btn ${prodSubTab === 'bom' ? 'active' : ''}" id="prodSubTabBom">Recipe BOM Catalog</button>
        </div>

        <div class="card-container" id="prodViewContent"></div>
    `;

    document.getElementById('prodSubTabRuns').addEventListener('click', () => {
        prodSubTab = 'runs';
        renderProductionView();
    });
    document.getElementById('prodSubTabBom').addEventListener('click', () => {
        prodSubTab = 'bom';
        renderProductionView();
    });

    const contentDiv = document.getElementById('prodViewContent');

    if (prodSubTab === 'runs') {
        contentDiv.innerHTML = `
            <div style="padding: 16px; display:flex; justify-content: flex-end;">
                <button class="btn-primary" id="openAddOrderBtn">+ Start New Production Run</button>
            </div>
            <div class="table-responsive">
                <table>
                    <thead>
                        <tr>
                            <th>Order ID</th>
                            <th>Batch Number</th>
                            <th>BOM ID</th>
                            <th>Planned Target Qty</th>
                            <th>Actual Received Qty</th>
                            <th>Start Date</th>
                            <th>Status</th>
                            <th>Control Action</th>
                        </tr>
                    </thead>
                    <tbody id="ordersTableBody"></tbody>
                </table>
            </div>
        `;
        const bodyNode = document.getElementById('ordersTableBody');
        if (orderList.length === 0) {
            bodyNode.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-secondary);">No production orders scheduled</td></tr>`;
        } else {
            orderList.forEach(item => {
                const tr = document.createElement('tr');
                let statusBadge = 'badge-warning';
                let actionBtn = '';
                
                const statusStr = item.status ? item.status.toString() : 'PLANNED';
                
                if (statusStr === 'PLANNED' || statusStr === 'Planned') {
                    statusBadge = 'badge-warning';
                    actionBtn = `<button class="btn-primary start-run-btn" data-id="${item.orderId}" style="padding:4px 8px; font-size:10px;">⚡ Allocate & Start</button>`;
                } else if (statusStr === 'IN_PRODUCTION' || statusStr === 'In-Production') {
                    statusBadge = 'badge-success';
                    actionBtn = `<button class="btn-secondary qa-test-btn" data-id="${item.orderId}" style="padding:4px 8px; font-size:10px;">🛡️ Send to QA</button>`;
                } else if (statusStr === 'QUALITY_TESTING' || statusStr === 'Quality-Testing') {
                    statusBadge = 'badge-warning';
                    actionBtn = `<span style="font-size:10px; color:var(--text-secondary);">🔬 QA Auditing...</span>`;
                } else if (statusStr === 'APPROVED' || statusStr === 'Approved') {
                    statusBadge = 'badge-success';
                    actionBtn = `<span style="font-size:10px; color:var(--status-green);">✅ Completed & Released</span>`;
                } else if (statusStr === 'REJECTED' || statusStr === 'Rejected') {
                    statusBadge = 'badge-danger';
                    actionBtn = `<span style="font-size:10px; color:var(--status-red);">❌ Failed QC Audit</span>`;
                }

                tr.innerHTML = `
                    <td>${item.orderId}</td>
                    <td><strong>${item.batchNumber}</strong></td>
                    <td>BOM-#${item.bomId}</td>
                    <td>${item.plannedQty}</td>
                    <td>${item.actualQty || '-'}</td>
                    <td>${item.productionDate || '-'}</td>
                    <td><span class="badge ${statusBadge}">${statusStr}</span></td>
                    <td>${actionBtn}</td>
                `;
                bodyNode.appendChild(tr);
            });
        }

        document.getElementById('openAddOrderBtn').addEventListener('click', () => {
            launchAddOrderModal();
        });
        document.querySelectorAll('.start-run-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = parseInt(e.currentTarget.getAttribute('data-id'));
                startProductionOrder(id);
            });
        });
        document.querySelectorAll('.qa-test-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = parseInt(e.currentTarget.getAttribute('data-id'));
                sendToQATesting(id);
            });
        });
    } else {
        contentDiv.innerHTML = `
            <div style="padding: 16px; display:flex; justify-content: flex-end;">
                <button class="btn-primary" id="openAddBomBtn">+ Define New BOM Formulation</button>
            </div>
            <div class="table-responsive">
                <table>
                    <thead>
                        <tr>
                            <th>BOM ID</th>
                            <th>Finished Product</th>
                            <th>Description</th>
                            <th>Ver</th>
                            <th>Status</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody id="bomTableBody"></tbody>
                </table>
            </div>
        `;
        const bodyNode = document.getElementById('bomTableBody');
        if (bomList.length === 0) {
            bodyNode.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-secondary);">No BOM formulations declared</td></tr>`;
        } else {
            bomList.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>BOM-#${item.bomId}</td>
                    <td><code>${item.materialCode}</code></td>
                    <td><strong>${item.description}</strong></td>
                    <td>v${item.versionNumber}</td>
                    <td><span class="badge badge-success">${item.active ? 'ACTIVE' : 'INACTIVE'}</span></td>
                    <td>
                        <button class="action-btn inspect-bom-btn" data-id="${item.bomId}">🔍 Details</button>
                    </td>
                `;
                bodyNode.appendChild(tr);
            });
        }

        document.getElementById('openAddBomBtn').addEventListener('click', () => {
            launchAddBomModal();
        });
        document.querySelectorAll('.inspect-bom-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const id = parseInt(e.currentTarget.getAttribute('data-id'));
                await inspectBOMIngredients(id);
            });
        });
    }
}

// 6. QA Compliance Inspections View
function renderComplianceView(data) {
    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">QA Quarantine Inspections</h1>
        </div>

        <div class="card-container">
            <div class="table-responsive">
                <table>
                    <thead>
                        <tr>
                            <th>Batch Number</th>
                            <th>Material Code</th>
                            <th>Location</th>
                            <th>Holding Quantity</th>
                            <th>Expiry Date</th>
                            <th>QC Status</th>
                            <th>Compliance Actions</th>
                        </tr>
                    </thead>
                    <tbody id="complianceTableBody"></tbody>
                </table>
            </div>
        </div>
    `;

    const bodyNode = document.getElementById('complianceTableBody');
    bodyNode.innerHTML = '';

    if (data.length === 0) {
        bodyNode.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-secondary);">No batches currently held in QA quarantine</td></tr>`;
    } else {
        data.forEach(item => {
            const tr = document.createElement('tr');
            
            tr.innerHTML = `
                <td><strong>${item.batchNumber}</strong></td>
                <td>${item.materialCode}</td>
                <td><code>${item.locationCode}</code></td>
                <td>${item.quantity}</td>
                <td>${item.expDate || '-'}</td>
                <td><span class="badge badge-warning">${item.qcStatus}</span></td>
                <td>
                    <button class="btn-primary inspect-batch-btn" data-batch="${item.batchNumber}" style="padding:4px 10px; font-size:10px;">🔬 Run Inspections</button>
                </td>
            `;
            bodyNode.appendChild(tr);
        });
    }

    document.querySelectorAll('.inspect-batch-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const batch = e.currentTarget.getAttribute('data-batch');
            launchInspectBatchModal(batch);
        });
    });
}

/* --- MODAL FORMS DATA LOADERS --- */

function launchInspectBatchModal(batchNumber) {
    const item = qaList.find(q => q.batchNumber === batchNumber);
    if (!item) return;

    document.getElementById('inspectBatchNumber').value = item.batchNumber;
    document.getElementById('inspectMaterialCode').value = item.materialCode;
    document.getElementById('inspectQuantity').value = item.quantity;
    document.getElementById('inspectRemarks').value = '';

    openModal('inspectBatchModal');
}

async function launchAddOrderModal() {
    const selectBOM = document.getElementById('addOrderBomId');
    selectBOM.innerHTML = '';
    
    if (bomList.length === 0) {
        try {
            const res = await fetch(`${API_BASE}/bom`);
            if (res.ok) bomList = await res.json();
        } catch (e) {}
    }

    if (bomList.length === 0) {
        selectBOM.innerHTML = `<option value="">-- No BOM Recipes Available --</option>`;
    } else {
        bomList.forEach(b => {
            selectBOM.innerHTML += `<option value="${b.bomId}">${b.description} (v${b.versionNumber}) for ${b.materialCode}</option>`;
        });
    }
    openModal('addProductionOrderModal');
}

async function launchAddBomModal() {
    const selectProduct = document.getElementById('addBomMaterialCode');
    const selectIng1 = document.getElementById('bomIng1');
    const selectIng2 = document.getElementById('bomIng2');

    selectProduct.innerHTML = '';
    selectIng1.innerHTML = '';
    selectIng2.innerHTML = `<option value="">-- None --</option>`;

    if (materials.length === 0) {
        try {
            const res = await fetch(`${API_BASE}/materials`);
            if (res.ok) materials = await res.json();
        } catch (e) {}
    }

    const finishedGoods = materials.filter(m => m.materialType === 'FINISHED_GOOD');
    const rawMaterials = materials.filter(m => m.materialType === 'RAW_MATERIAL' || m.materialType === 'EXCIPIENT' || m.materialType === 'PACKAGING');

    if (finishedGoods.length === 0) {
        selectProduct.innerHTML = `<option value="">-- No Active Finished Goods Found --</option>`;
    } else {
        finishedGoods.forEach(m => {
            selectProduct.innerHTML += `<option value="${m.materialCode}">${m.brandName} (${m.materialCode})</option>`;
        });
    }

    if (rawMaterials.length === 0) {
        selectIng1.innerHTML = `<option value="">-- No Raw Materials Available --</option>`;
    } else {
        rawMaterials.forEach(m => {
            const option = `<option value="${m.materialCode}">${m.brandName} (${m.materialCode})</option>`;
            selectIng1.innerHTML += option;
            selectIng2.innerHTML += option;
        });
    }

    openModal('addBomModal');
}

async function inspectBOMIngredients(bomId) {
    try {
        const res = await fetch(`${API_BASE}/bom/${bomId}/ingredients`);
        if (!res.ok) throw new Error("Failed to load ingredients");
        const ingredients = await res.json();
        
        let details = `Ingredients Details for BOM #${bomId}:\n\n`;
        ingredients.forEach(i => {
            details += `- Ingredient Code: ${i.ingredientMaterialCode}\n  Qty required (per unit): ${i.requiredQty} ${i.uom}\n  Notes: ${i.notes || '-'}\n\n`;
        });
        alert(details);
    } catch (e) {
        alert("Error loading ingredients: " + e.message);
    }
}

/* --- MATERIAL & SUPPLIER BINDINGS --- */
function launchEditMaterialModal(code) {
    const item = materials.find(m => m.materialCode === code);
    if (!item) return;

    editTargetMaterialCode = code;
    document.getElementById('editMaterialCode').value = item.materialCode;
    document.getElementById('editBrandName').value = item.brandName;
    document.getElementById('editGenericName').value = item.genericName || '';
    document.getElementById('editManufacturer').value = item.manufacturer || '';
    document.getElementById('editFormulation').value = item.formulation || '';
    document.getElementById('editStrength').value = item.strength || '';
    document.getElementById('editMaterialType').value = item.materialType;
    document.getElementById('editUnitOfMeasure').value = item.unitOfMeasure;
    document.getElementById('editReorderLevel').value = item.reorderLevel || 100;
    document.getElementById('editIsActive').checked = item.active;

    openModal('editMaterialModal');
}

function launchEditSupplierModal(id) {
    const item = suppliers.find(s => s.supplierId === id);
    if (!item) return;

    editTargetSupplierId = id;
    document.getElementById('editSupplierId').value = item.supplierId;
    document.getElementById('editSupplierName').value = item.supplierName;
    document.getElementById('editContactPerson').value = item.contactPerson || '';
    document.getElementById('editSupplierPhone').value = item.phone || '';
    document.getElementById('editSupplierEmail').value = item.email || '';
    document.getElementById('editSupplierPaymentTerms').value = item.paymentTerms || '';
    document.getElementById('editSupplierGstin').value = item.gstin;
    document.getElementById('editSupplierLicense').value = item.drugLicenseNo;
    document.getElementById('editSupplierAddress').value = item.address || '';

    openModal('editSupplierModal');
}

function launchStatusModal(id, status) {
    document.getElementById('statusSupplierId').value = id;
    document.getElementById('statusTargetValue').value = status;
    document.getElementById('statusModalTitle').textContent = `${status === 'APPROVED' ? 'Approve' : 'Reject'} Supplier compliance`;
    document.getElementById('statusSubmitBtn').textContent = status === 'APPROVED' ? 'Approve Vendor' : 'Reject Vendor';
    openModal('supplierStatusModal');
}

/* --- FORMS SUBMIT LISTENERS --- */
function initFormSubmitListeners() {
    // Add Material
    addMaterialForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const payload = {
            materialCode: document.getElementById('addMaterialCode').value.trim(),
            brandName: document.getElementById('addBrandName').value.trim(),
            genericName: document.getElementById('addGenericName').value.trim(),
            manufacturer: document.getElementById('addManufacturer').value.trim(),
            formulation: document.getElementById('addFormulation').value.trim(),
            strength: document.getElementById('addStrength').value.trim(),
            materialType: document.getElementById('addMaterialType').value,
            unitOfMeasure: document.getElementById('addUnitOfMeasure').value,
            reorderLevel: parseInt(document.getElementById('addReorderLevel').value),
            active: document.getElementById('addIsActive').checked
        };
        createMaterial(payload);
    });

    // Edit Material
    editMaterialForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const payload = {
            materialCode: editTargetMaterialCode,
            brandName: document.getElementById('editBrandName').value.trim(),
            genericName: document.getElementById('editGenericName').value.trim(),
            manufacturer: document.getElementById('editManufacturer').value.trim(),
            formulation: document.getElementById('editFormulation').value.trim(),
            strength: document.getElementById('editStrength').value.trim(),
            materialType: document.getElementById('editMaterialType').value,
            unitOfMeasure: document.getElementById('editUnitOfMeasure').value,
            reorderLevel: parseInt(document.getElementById('editReorderLevel').value),
            active: document.getElementById('editIsActive').checked
        };
        updateMaterial(editTargetMaterialCode, payload);
    });

    // Add Supplier
    addSupplierForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const payload = {
            supplierName: document.getElementById('addSupplierName').value.trim(),
            contactPerson: document.getElementById('addContactPerson').value.trim(),
            phone: document.getElementById('addSupplierPhone').value.trim(),
            email: document.getElementById('addSupplierEmail').value.trim(),
            paymentTerms: document.getElementById('addSupplierPaymentTerms').value.trim(),
            gstin: document.getElementById('addSupplierGstin').value.trim(),
            drugLicenseNo: document.getElementById('addSupplierLicense').value.trim(),
            address: document.getElementById('addSupplierAddress').value.trim(),
            supplierStatus: "PENDING"
        };
        createSupplier(payload);
    });

    // Edit Supplier
    editSupplierForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const payload = {
            supplierId: editTargetSupplierId,
            supplierName: document.getElementById('editSupplierName').value.trim(),
            contactPerson: document.getElementById('editContactPerson').value.trim(),
            phone: document.getElementById('editSupplierPhone').value.trim(),
            email: document.getElementById('editSupplierEmail').value.trim(),
            paymentTerms: document.getElementById('editSupplierPaymentTerms').value.trim(),
            gstin: document.getElementById('editSupplierGstin').value.trim(),
            drugLicenseNo: document.getElementById('editSupplierLicense').value.trim(),
            address: document.getElementById('editSupplierAddress').value.trim()
        };
        updateSupplier(editTargetSupplierId, payload);
    });

    // Supplier Status Update
    supplierStatusForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const id = parseInt(document.getElementById('statusSupplierId').value);
        const status = document.getElementById('statusTargetValue').value;
        const remarks = document.getElementById('statusRemarks').value.trim();
        const user = document.getElementById('statusPerformedBy').value.trim();
        patchSupplierStatus(id, status, remarks, user);
    });

    // Add Production Order
    addProductionOrderForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const payload = {
            batchNumber: document.getElementById('addOrderBatchNumber').value.trim(),
            bomId: parseInt(document.getElementById('addOrderBomId').value),
            plannedQty: parseFloat(document.getElementById('addOrderPlannedQty').value),
            createdBy: parseInt(document.getElementById('addOrderCreatedBy').value),
            notes: document.getElementById('addOrderNotes').value.trim()
        };
        createProductionOrder(payload);
    });

    // Add BOM Form
    addBomForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const header = {
            materialCode: document.getElementById('addBomMaterialCode').value,
            description: document.getElementById('addBomDescription').value.trim(),
            versionNumber: parseInt(document.getElementById('addBomVersion').value),
            active: true
        };
        const details = [];
        const ing1Code = document.getElementById('bomIng1').value;
        const ing1Qty = parseFloat(document.getElementById('bomIng1Qty').value);
        const ing1Uom = document.getElementById('bomIng1Uom').value;
        details.push({
            ingredientMaterialCode: ing1Code,
            requiredQty: ing1Qty,
            uom: ing1Uom,
            sequenceNumber: 1,
            notes: "Primary raw material"
        });
        const ing2Code = document.getElementById('bomIng2').value;
        const ing2QtyVal = document.getElementById('bomIng2Qty').value;
        if (ing2Code && ing2QtyVal) {
            details.push({
                ingredientMaterialCode: ing2Code,
                requiredQty: parseFloat(ing2QtyVal),
                uom: document.getElementById('bomIng2Uom').value,
                sequenceNumber: 2,
                notes: "Secondary additive"
            });
        }
        createBom({ header, details });
    });

    // QA Inspection Decision Form
    inspectBatchForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const batch = document.getElementById('inspectBatchNumber').value;
        const decision = document.getElementById('inspectQcDecision').value;
        const remarks = document.getElementById('inspectRemarks').value.trim();
        const user = document.getElementById('inspectPerformedBy').value.trim();

        submitQADecision(batch, decision, remarks, user);
    });
}

/* --- SEARCH FILTER (ROUTED TO CURRENT VIEW) --- */
searchInput.addEventListener('input', () => {
    const query = searchInput.value.toLowerCase().trim();

    if (currentView === 'materials') {
        const filtered = materials.filter(item => {
            return item.materialCode.toLowerCase().includes(query) ||
                   item.brandName.toLowerCase().includes(query);
        });
        renderMaterialsView(filtered);
    } else if (currentView === 'suppliers') {
        const filtered = suppliers.filter(item => {
            return item.supplierName.toLowerCase().includes(query) ||
                   item.gstin.toLowerCase().includes(query);
        });
        renderSuppliersView(filtered);
    } else if (currentView === 'inventory') {
        if (stockSubTab === 'current') {
            const filtered = stockList.filter(item => {
                return item.batchNumber.toLowerCase().includes(query) ||
                       item.materialCode.toLowerCase().includes(query);
            });
            const orig = stockList;
            stockList = filtered;
            renderStockView();
            stockList = orig;
        }
    } else if (currentView === 'production') {
        if (prodSubTab === 'runs') {
            const filtered = orderList.filter(item => {
                return item.batchNumber.toLowerCase().includes(query) ||
                       item.status.toString().toLowerCase().includes(query);
            });
            const orig = orderList;
            orderList = filtered;
            renderProductionView();
            orderList = orig;
        } else {
            const filtered = bomList.filter(item => {
                return item.description.toLowerCase().includes(query) ||
                       item.materialCode.toLowerCase().includes(query);
            });
            const orig = bomList;
            bomList = filtered;
            renderProductionView();
            bomList = orig;
        }
    } else if (currentView === 'compliance') {
        const filtered = qaList.filter(item => {
            return item.batchNumber.toLowerCase().includes(query) ||
                   item.materialCode.toLowerCase().includes(query);
        });
        renderComplianceView(filtered);
    }
});

/* --- FLOATING AI WIDGET --- */
xaiTrigger.addEventListener('click', () => {
    xaiPanel.style.display = 'flex';
    xaiPulse.style.display = 'none';
});

xaiClose.addEventListener('click', () => {
    xaiPanel.style.display = 'none';
});

tabLogs.addEventListener('click', () => {
    tabLogs.classList.add('active');
    tabChat.classList.remove('active');
    viewLogs.classList.add('active');
    viewChat.classList.remove('active');
});

tabChat.addEventListener('click', () => {
    tabLogs.classList.remove('active');
    tabChat.classList.add('active');
    viewLogs.classList.remove('active');
    viewChat.classList.add('active');
});

function appendChatMessage(sender, text) {
    const msg = document.createElement('div');
    msg.className = `chat-msg ${sender}`;
    msg.innerHTML = `
        ${text}
        <div class="chat-msg-time">Just now</div>
    `;
    chatList.appendChild(msg);
    chatList.scrollTop = chatList.scrollHeight;
}

function parseMarkdownSimple(text) {
    if (!text) return '';
    let html = text;
    html = html.replace(/^### (.*$)/gim, '<h3 style="font-size:12px; font-weight:600; margin: 8px 0 4px 0; color:var(--accent-teal);">$1</h3>');
    html = html.replace(/^## (.*$)/gim, '<h2 style="font-size:13px; font-weight:600; margin: 10px 0 6px 0; color:var(--text-primary);">$1</h2>');
    html = html.replace(/^# (.*$)/gim, '<h1 style="font-size:14px; font-weight:700; margin: 12px 0 8px 0; color:var(--text-primary);">$1</h1>');
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/`(.*?)`/g, '<code style="background-color:rgba(255,255,255,0.06); padding:2px 4px; border-radius:4px; font-family:monospace;">$1</code>');
    html = html.replace(/^\s*-\s+(.*$)/gim, '<li style="margin-left: 12px; list-style-type: disc;">$1</li>');
    html = html.replace(/\n/g, '<br>');
    return html;
}

async function handleChatSubmit() {
    const text = chatInput.value.trim();
    if (!text) return;

    appendChatMessage('user', text);
    chatInput.value = '';

    const thinkingId = 'chat-thinking-' + Date.now();
    const thinkingMsg = document.createElement('div');
    thinkingMsg.className = 'chat-msg bot';
    thinkingMsg.id = thinkingId;
    thinkingMsg.innerHTML = `
        <span style="font-style: italic; opacity: 0.7;">Co-pilot is thinking...</span>
        <div class="chat-msg-time">Just now</div>
    `;
    chatList.appendChild(thinkingMsg);
    chatList.scrollTop = chatList.scrollHeight;

    try {
        const res = await fetch(`${API_BASE}/agent/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: text })
        });
        if (!res.ok) throw new Error("Connection failed");
        const data = await res.json();
        
        const msgNode = document.getElementById(thinkingId);
        if (msgNode) {
            msgNode.innerHTML = `
                ${parseMarkdownSimple(data.reply)}
                <div class="chat-msg-time">Just now</div>
            `;
        }
    } catch (err) {
        const msgNode = document.getElementById(thinkingId);
        if (msgNode) {
            msgNode.innerHTML = `
                <span style="color: var(--status-red);">⚠️ Error connecting to JADE agent gateway.</span>
                <div class="chat-msg-time">Just now</div>
            `;
        }
    }
}

function initSSE() {
    const eventSource = new EventSource(`${API_BASE}/agent/stream`);
    
    eventSource.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            appendLiveLogItem(data);
        } catch (e) {
            console.error("Error parsing SSE event: ", e);
        }
    };
    
    eventSource.onerror = (err) => {
        console.error("SSE stream error: ", err);
    };
}

function appendLiveLogItem(el) {
    const logList = document.getElementById('logList');
    if (!logList) return;

    if (logList.innerHTML.includes("Waiting for Javalin REST SSE connection")) {
        logList.innerHTML = '';
    }

    const item = document.createElement('div');
    
    let typeClass = 'procurement';
    if (el.eventType.includes('QC') || el.eventType.includes('QA') || el.entityType.includes('BATCH')) typeClass = 'compliance';
    if (el.eventType.includes('PRODUCTION') || el.entityType.includes('PRODUCTION')) typeClass = 'qa';
    if (el.eventType.includes('STOCK') || el.entityType.includes('STOCK')) typeClass = 'inventory';

    const timeStr = new Date().toLocaleTimeString();

    item.className = `log-item ${typeClass}`;
    item.innerHTML = `
        <div class="log-text">
            <div class="log-meta">
                <span class="log-agent">${el.eventType} (${el.entityType})</span>
                <span class="log-time">${timeStr}</span>
            </div>
            ${el.details}
        </div>
    `;
    
    logList.insertBefore(item, logList.firstChild);
    
    if (xaiPanel.style.display !== 'flex') {
        xaiPulse.style.display = 'block';
    }
}

sendChatBtn.addEventListener('click', handleChatSubmit);
chatInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') handleChatSubmit();
});

/* --- INITIALIZATION LAUNCHER --- */
function init() {
    initRouter();
    initFormSubmitListeners();
    loadViewData('materials');
    initSSE();
    setInterval(() => {
        loadViewData(currentView);
    }, 8000);
}

document.addEventListener('DOMContentLoaded', init);
if (document.readyState === 'interactive' || document.readyState === 'complete') {
    init();
}
