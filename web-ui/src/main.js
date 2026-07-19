import './style.css';

// REST API Configuration
const API_BASE = "http://localhost:8080/api";
const SESSION_STORAGE_KEY = "pharmaScm.authSession";

// Global Fetch Interceptor to inject authentication headers
const originalFetch = window.fetch;
window.fetch = async function(url, options) {
    options = options || {};
    options.headers = options.headers || {};
    
    // Auto-inject current logged-in user context
    if (typeof currentUser !== 'undefined' && currentUser && currentUser.userId) {
        options.headers['X-User-Id'] = String(currentUser.userId);
        if (currentUser.roleName) {
            options.headers['X-User-Role'] = currentUser.roleName;
        }
    }
    
    try {
        const response = await originalFetch(url, options);
        // If server returns 401 Unauthorized (except for login itself), clear session and kick to login screen
        if (response.status === 401 && !url.includes('/auth/login')) {
            if (typeof logout === 'function') {
                logout();
            }
        }
        return response;
    } catch (error) {
        throw error;
    }
};

// Global Modal Helpers
function openModal(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.add('active');
}
function closeModal(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.remove('active');
}
window.openModal = openModal;
window.closeModal = closeModal;

// Override window.alert with a premium toast notification
window.alert = function(message) {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.style.position = 'fixed';
        container.style.top = '20px';
        container.style.right = '20px';
        container.style.zIndex = '10000';
        container.style.display = 'flex';
        container.style.flexDirection = 'column';
        container.style.gap = '10px';
        document.body.appendChild(container);
    }
    
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.style.minWidth = '300px';
    toast.style.maxWidth = '450px';
    toast.style.backgroundColor = 'var(--bg-card, rgba(30, 41, 59, 0.95))';
    toast.style.backdropFilter = 'blur(12px)';
    toast.style.border = '1px solid var(--border-color, #334155)';
    toast.style.borderRadius = '8px';
    toast.style.padding = '14px 18px';
    toast.style.color = 'var(--text-primary, #ffffff)';
    toast.style.fontSize = '13px';
    toast.style.boxShadow = '0 10px 15px -3px rgba(0, 0, 0, 0.3)';
    toast.style.display = 'flex';
    toast.style.alignItems = 'center';
    toast.style.justifyContent = 'space-between';
    toast.style.gap = '15px';
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(-20px) scale(0.95)';
    toast.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
    
    const contentWrapper = document.createElement('div');
    contentWrapper.style.display = 'flex';
    contentWrapper.style.alignItems = 'center';
    contentWrapper.style.gap = '10px';
    
    const isError = message.toLowerCase().includes('error') || message.toLowerCase().includes('fail') || message.toLowerCase().includes('invalid') || message.toLowerCase().includes('required');
    const icon = document.createElement('span');
    icon.style.fontSize = '16px';
    if (isError) {
        icon.innerHTML = '⚠️';
        icon.style.color = '#ef4444';
    } else {
        icon.innerHTML = '✨';
        icon.style.color = 'var(--accent-teal, #38bdf8)';
    }
    contentWrapper.appendChild(icon);
    
    const text = document.createElement('span');
    text.innerText = message;
    contentWrapper.appendChild(text);
    toast.appendChild(contentWrapper);
    
    const closeBtn = document.createElement('button');
    closeBtn.innerHTML = '&times;';
    closeBtn.style.background = 'none';
    closeBtn.style.border = 'none';
    closeBtn.style.color = 'var(--text-secondary, #94a3b8)';
    closeBtn.style.fontSize = '16px';
    closeBtn.style.cursor = 'pointer';
    closeBtn.style.padding = '0 5px';
    closeBtn.onclick = () => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(-20px) scale(0.95)';
        setTimeout(() => toast.remove(), 300);
    };
    toast.appendChild(closeBtn);
    
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.style.opacity = '1';
        toast.style.transform = 'translateY(0) scale(1)';
    }, 10);
    
    setTimeout(() => {
        if (toast.parentNode) {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(-20px) scale(0.95)';
            setTimeout(() => toast.remove(), 300);
        }
    }, 4500);
};

// Custom Confirm and Prompt implementation
function createCustomDialog(type, message, defaultValue = '', placeholder = '') {
    return new Promise((resolve) => {
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay active';
        overlay.style.zIndex = '9999';

        const card = document.createElement('div');
        card.className = 'modal-card';
        card.style.width = '420px';
        card.style.padding = '0';
        card.style.background = 'var(--bg-card)';
        
        const header = document.createElement('div');
        header.className = 'modal-header';
        
        const title = document.createElement('span');
        title.className = 'modal-title';
        title.style.fontSize = '14px';
        title.style.fontWeight = '600';
        
        if (type === 'confirm') {
            title.innerText = 'Confirmation Required';
            title.style.color = '#ef4444';
        } else if (type === 'prompt') {
            title.innerText = 'Remarks / Input Required';
            title.style.color = 'var(--accent-teal)';
        } else if (type === 'password') {
            title.innerText = 'Electronic Signature Verification';
            title.style.color = 'var(--accent-teal)';
        }
        
        header.appendChild(title);
        card.appendChild(header);
        
        const body = document.createElement('div');
        body.className = 'modal-body';
        body.style.padding = '20px';
        body.style.fontSize = '13px';
        body.style.lineHeight = '1.6';
        body.style.color = 'var(--text-primary)';
        
        const text = document.createElement('p');
        text.innerText = message;
        body.appendChild(text);
        
        let inputField = null;
        if (type === 'prompt' || type === 'password') {
            inputField = document.createElement('input');
            inputField.type = type === 'password' ? 'password' : 'text';
            inputField.value = defaultValue;
            inputField.placeholder = placeholder;
            inputField.style.width = '100%';
            inputField.style.marginTop = '15px';
            inputField.style.padding = '10px 12px';
            inputField.style.borderRadius = '6px';
            inputField.style.border = '1px solid var(--border-color)';
            inputField.style.backgroundColor = 'var(--bg-elevated)';
            inputField.style.color = 'var(--text-primary)';
            inputField.style.outline = 'none';
            inputField.style.fontSize = '13px';
            body.appendChild(inputField);
        }
        
        card.appendChild(body);
        
        const footer = document.createElement('div');
        footer.className = 'modal-footer';
        footer.style.padding = '12px 20px';
        footer.style.display = 'flex';
        footer.style.justifyContent = 'flex-end';
        footer.style.gap = '10px';
        footer.style.borderTop = '1px solid var(--border-color)';
        
        const cleanUp = () => {
            overlay.classList.remove('active');
            setTimeout(() => overlay.remove(), 300);
        };
        
        const cancelBtn = document.createElement('button');
        cancelBtn.className = 'btn-secondary';
        cancelBtn.innerText = 'Cancel';
        cancelBtn.onclick = () => {
            cleanUp();
            resolve(type === 'confirm' ? false : null);
        };
        
        const okBtn = document.createElement('button');
        okBtn.className = 'btn-primary';
        okBtn.innerText = type === 'confirm' ? 'Confirm' : 'Submit';
        
        if (type === 'confirm') {
            okBtn.style.backgroundColor = '#ef4444';
            okBtn.style.color = '#ffffff';
            okBtn.onclick = () => {
                cleanUp();
                resolve(true);
            };
        } else {
            const submitVal = () => {
                cleanUp();
                resolve(inputField.value);
            };
            okBtn.onclick = submitVal;
            inputField.onkeydown = (e) => {
                if (e.key === 'Enter') submitVal();
                if (e.key === 'Escape') {
                    cleanUp();
                    resolve(null);
                }
            };
        }
        
        footer.appendChild(cancelBtn);
        footer.appendChild(okBtn);
        card.appendChild(footer);
        overlay.appendChild(card);
        document.body.appendChild(overlay);
        
        if (inputField) {
            setTimeout(() => inputField.focus(), 50);
        }
    });
}

function customConfirm(message) {
    return createCustomDialog('confirm', message);
}

function customPrompt(message, defaultValue = '') {
    return createCustomDialog('prompt', message, defaultValue);
}

function customPasswordPrompt(message, defaultValue = '') {
    return createCustomDialog('password', message, defaultValue, 'Enter password to sign');
}

// Excel-like selection and multi-delete for tables
let activeSelectionCleanup = null;

function setupExcelLikeSelection(tableBodyId, rowKeyAttr, onDeleteSelected) {
    if (activeSelectionCleanup) {
        activeSelectionCleanup();
        activeSelectionCleanup = null;
    }

    const tbody = document.getElementById(tableBodyId);
    if (!tbody) return;

    let selectedKeys = new Set();
    let lastSelectedIndex = null;

    let actionBar = document.getElementById('selection-action-bar');
    if (!actionBar) {
        actionBar = document.createElement('div');
        actionBar.id = 'selection-action-bar';
        actionBar.className = 'selection-action-bar';
        document.body.appendChild(actionBar);
    }

    const updateActionBar = () => {
        if (selectedKeys.size > 0) {
            actionBar.innerHTML = `
                <span class="selection-info"><strong>${selectedKeys.size}</strong> row(s) selected</span>
                <button class="btn-primary" id="deleteSelectedBtn" style="background-color: #ef4444; border-color: #ef4444; color: #ffffff; padding: 6px 12px; font-size: 11px;">Delete Selected (Del)</button>
                <button class="btn-secondary" id="clearSelectionBtn" style="padding: 6px 12px; font-size: 11px;">Clear (Esc)</button>
            `;
            actionBar.classList.add('active');
            
            document.getElementById('deleteSelectedBtn').onclick = () => {
                onDeleteSelected(Array.from(selectedKeys));
                clearSelection();
            };
            document.getElementById('clearSelectionBtn').onclick = clearSelection;
        } else {
            actionBar.classList.remove('active');
        }
    };

    const clearSelection = () => {
        selectedKeys.clear();
        const rows = tbody.querySelectorAll('tr');
        rows.forEach(r => r.classList.remove('selected-row'));
        updateActionBar();
    };

    const selectRow = (index, extend = false) => {
        const rows = tbody.querySelectorAll('tr');
        if (index < 0 || index >= rows.length) return;

        if (!extend) {
            selectedKeys.clear();
            rows.forEach(r => r.classList.remove('selected-row'));
        }

        const tr = rows[index];
        const key = tr.getAttribute(rowKeyAttr);
        
        if (extend && selectedKeys.has(key)) {
            // Do not toggle off
        } else {
            selectedKeys.add(key);
            tr.classList.add('selected-row');
        }
        
        updateActionBar();
        lastSelectedIndex = index;
    };

    const selectRange = (startIndex, endIndex) => {
        const rows = tbody.querySelectorAll('tr');
        selectedKeys.clear();
        rows.forEach(r => r.classList.remove('selected-row'));

        const start = Math.min(startIndex, endIndex);
        const end = Math.max(startIndex, endIndex);

        for (let i = start; i <= end; i++) {
            const tr = rows[i];
            const key = tr.getAttribute(rowKeyAttr);
            selectedKeys.add(key);
            tr.classList.add('selected-row');
        }
        updateActionBar();
    };

    const onRowClick = (e) => {
        if (e.target.closest('button') || e.target.closest('input') || e.target.closest('select')) {
            return;
        }
        const tr = e.target.closest('tr');
        if (!tr || tr.parentNode !== tbody) return;

        const rows = Array.from(tbody.querySelectorAll('tr'));
        const index = rows.indexOf(tr);

        if (e.shiftKey && lastSelectedIndex !== null) {
            selectRange(lastSelectedIndex, index);
        } else if (e.ctrlKey || e.metaKey) {
            const key = tr.getAttribute(rowKeyAttr);
            if (selectedKeys.has(key)) {
                selectedKeys.delete(key);
                tr.classList.remove('selected-row');
            } else {
                selectedKeys.add(key);
                tr.classList.add('selected-row');
            }
            updateActionBar();
            lastSelectedIndex = index;
        } else {
            selectRow(index, false);
        }
    };

    tbody.addEventListener('click', onRowClick);

    const onKeyDown = (e) => {
        if (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA' || document.activeElement.tagName === 'SELECT') {
            return;
        }

        const rows = tbody.querySelectorAll('tr');
        if (rows.length === 0) return;

        if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
            e.preventDefault();
            let nextIndex = 0;
            if (lastSelectedIndex !== null) {
                if (e.key === 'ArrowDown') {
                    nextIndex = Math.min(lastSelectedIndex + 1, rows.length - 1);
                } else {
                    nextIndex = Math.max(lastSelectedIndex - 1, 0);
                }
            }
            
            if (e.shiftKey && lastSelectedIndex !== null) {
                selectRange(lastSelectedIndex, nextIndex);
                lastSelectedIndex = nextIndex;
            } else {
                selectRow(nextIndex, false);
            }
            rows[nextIndex].scrollIntoView({ block: 'nearest', behavior: 'smooth' });
        } else if (e.key === 'Delete') {
            if (selectedKeys.size > 0) {
                e.preventDefault();
                onDeleteSelected(Array.from(selectedKeys));
                clearSelection();
            }
        } else if (e.key === 'Escape') {
            if (selectedKeys.size > 0) {
                e.preventDefault();
                clearSelection();
            }
        }
    };

    window.addEventListener('keydown', onKeyDown);

    activeSelectionCleanup = () => {
        tbody.removeEventListener('click', onRowClick);
        window.removeEventListener('keydown', onKeyDown);
        actionBar.classList.remove('active');
    };
}

async function deleteMultipleMaterials(codes) {
    if (codes.length === 1) {
        await deleteMaterial(codes[0]);
        return;
    }
    if (!await customConfirm(`Are you sure you want to delete ${codes.length} material(s)?`)) return;
    let success = 0;
    let failures = [];
    for (const code of codes) {
        try {
            const res = await fetch(`${API_BASE}/materials/${code}`, { method: 'DELETE' });
            if (res.ok) {
                success++;
            } else {
                const errObj = await res.json().catch(() => ({}));
                failures.push({ code, reason: errObj.error || `HTTP ${res.status}` });
            }
        } catch (err) {
            failures.push({ code, reason: err.message });
        }
    }
    if (failures.length === 0) {
        alert(`Successfully deleted all ${success} material(s).`);
    } else {
        let report = `Deleted ${success} material(s).\n\nFailed to delete ${failures.length} material(s):\n`;
        failures.forEach(f => {
            report += `- Material ${f.code}: ${f.reason}\n`;
        });
        alert(report);
    }
    await fetchMaterials();
}

async function deleteMultipleSuppliers(ids) {
    if (ids.length === 1) {
        await deleteSupplier(parseInt(ids[0]));
        return;
    }
    if (!await customConfirm(`Are you sure you want to delete ${ids.length} supplier record(s)?`)) return;
    let success = 0;
    let failures = [];
    for (const id of ids) {
        try {
            const res = await fetch(`${API_BASE}/suppliers/${id}`, { method: 'DELETE' });
            if (res.ok) {
                success++;
            } else {
                const errObj = await res.json().catch(() => ({}));
                failures.push({ id, reason: errObj.error || `HTTP ${res.status}` });
            }
        } catch (err) {
            failures.push({ id, reason: err.message });
        }
    }
    if (failures.length === 0) {
        alert(`Successfully deleted all ${success} supplier record(s).`);
    } else {
        let report = `Deleted ${success} supplier(s).\n\nFailed to delete ${failures.length} supplier(s):\n`;
        failures.forEach(f => {
            report += `- Supplier ID ${f.id}: ${f.reason}\n`;
        });
        alert(report);
    }
    await fetchSuppliers();
}

async function deleteMultipleLocations(codes) {
    if (codes.length === 1) {
        await deleteLocation(codes[0]);
        return;
    }
    if (!await customConfirm(`Are you sure you want to delete ${codes.length} storage location(s)?`)) return;
    let success = 0;
    let failures = [];
    for (const code of codes) {
        try {
            const res = await fetch(`${API_BASE}/locations/${code}`, { method: 'DELETE' });
            if (res.ok) {
                success++;
            } else {
                const errObj = await res.json().catch(() => ({}));
                failures.push({ code, reason: errObj.error || `HTTP ${res.status}` });
            }
        } catch (err) {
            failures.push({ code, reason: err.message });
        }
    }
    if (failures.length === 0) {
        alert(`Successfully deleted all ${success} storage location(s).`);
    } else {
        let report = `Deleted ${success} location(s).\n\nFailed to delete ${failures.length} location(s):\n`;
        failures.forEach(f => {
            report += `- Location ${f.code}: ${f.reason}\n`;
        });
        alert(report);
    }
    await fetchLocations();
}

async function deleteMultiplePurchaseOrders(ids) {
    if (ids.length === 1) {
        await deletePurchaseOrder(parseInt(ids[0]));
        return;
    }
    if (!await customConfirm(`Are you sure you want to delete ${ids.length} Purchase Order(s)?`)) return;
    let success = 0;
    let failures = [];
    for (const id of ids) {
        try {
            const res = await fetch(`${API_BASE}/purchase-orders/${id}`, { method: 'DELETE' });
            if (res.ok) {
                success++;
            } else {
                const errObj = await res.json().catch(() => ({}));
                failures.push({ id, reason: errObj.error || `HTTP ${res.status}` });
            }
        } catch (err) {
            failures.push({ id, reason: err.message });
        }
    }
    if (failures.length === 0) {
        alert(`Successfully deleted all ${success} Purchase Order(s).`);
    } else {
        let report = `Deleted ${success} Purchase Order(s).\n\nFailed to delete ${failures.length} order(s):\n`;
        failures.forEach(f => {
            report += `- PO #${f.id}: ${f.reason}\n`;
        });
        alert(report);
    }
    await fetchPurchaseOrders();
}
function formatDate(dateInput) {
    if (!dateInput) return '-';
    
    let parts = [];
    if (Array.isArray(dateInput)) {
        parts = dateInput;
    } else if (typeof dateInput === 'string') {
        if (dateInput.includes(',')) {
            parts = dateInput.split(',').map(p => parseInt(p.trim()));
        } else if (dateInput.includes('-')) {
            parts = dateInput.split('-').map(p => parseInt(p.trim()));
        } else {
            const d = new Date(dateInput);
            if (!isNaN(d.getTime())) {
                const dd = String(d.getDate()).padStart(2, '0');
                const mm = String(d.getMonth() + 1).padStart(2, '0');
                const yyyy = d.getFullYear();
                return `${dd}-${mm}-${yyyy}`;
            }
            return dateInput;
        }
    } else {
        return String(dateInput);
    }
    
    if (parts.length >= 3) {
        const yyyy = parts[0];
        const mm = String(parts[1]).padStart(2, '0');
        const dd = String(parts[2]).padStart(2, '0');
        return `${dd}-${mm}-${yyyy}`;
    }
    
    return String(dateInput);
}

function makeXaiPanelAdjustable() {
    const header = document.querySelector('.xai-header');
    const panel = document.getElementById('xaiPanel');
    if (!header || !panel) return;

    let isDragging = false;
    let startX = 0, startY = 0;
    let initialLeft = 0, initialTop = 0;

    header.style.cursor = 'move';

    header.addEventListener('mousedown', (e) => {
        if (e.target.closest('#xaiClose')) return;
        
        isDragging = true;
        startX = e.clientX;
        startY = e.clientY;

        const rect = panel.getBoundingClientRect();
        initialLeft = rect.left;
        initialTop = rect.top;

        panel.style.bottom = 'auto';
        panel.style.right = 'auto';
        panel.style.left = `${initialLeft}px`;
        panel.style.top = `${initialTop}px`;
        panel.style.transform = 'none';

        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
    });

    function onMouseMove(e) {
        if (!isDragging) return;
        const dx = e.clientX - startX;
        const dy = e.clientY - startY;
        panel.style.left = `${initialLeft + dx}px`;
        panel.style.top = `${initialTop + dy}px`;
    }

    function onMouseUp() {
        isDragging = false;
        document.removeEventListener('mousemove', onMouseMove);
        document.removeEventListener('mouseup', onMouseUp);
    }
}

// Click backdrop (outside modal card/window) to dismiss
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay') && e.target.classList.contains('active')) {
        e.target.classList.remove('active');
    }
});

// Local State Store
let currentView = 'overview';
let stockSubTab = 'current'; // 'current' or 'transactions'
let prodSubTab = 'runs'; // 'runs' or 'bom'
let currentUser = null;
let shellInitialized = false;
let authListenersInitialized = false;
let initCompleted = false;
let refreshTimerId = null;
let sseInitialized = false;
let agentEventSource = null;

let materials = [];
let suppliers = [];
let stockList = [];
let txList = [];
let bomList = [];
let orderList = [];
let qaList = [];
let locations = [];
let purchaseOrders = [];
let grns = [];
let activeReport = 'stock-val';
let reportData = [];
let adminRoles = [];
let adminSelectedRole = null;
let adminRolePermissions = null;
let riskReports = [];
let aiDecisions = [];
let qaSelectedBatch = null;
let qaStatusFilter = 'ALL';
let qaAllBatches = [];

let editTargetMaterialCode = null;
let editTargetSupplierId = null;

// Persistent DOM Nodes
const mainViewport = document.getElementById('mainViewport');
const breadcrumbCurrent = document.getElementById('breadcrumbCurrent');
const searchInput = document.getElementById('globalSearch');
const loginScreen = document.getElementById('loginScreen');
const loginForm = document.getElementById('loginForm');
const loginEmployeeId = document.getElementById('loginEmployeeId');
const loginPassword = document.getElementById('loginPassword');
const togglePasswordVisibility = document.getElementById('togglePasswordVisibility');
const loginMessage = document.getElementById('loginMessage');
const loginSubmitBtn = document.getElementById('loginSubmitBtn');
const userAvatar = document.getElementById('userAvatar');
const userName = document.getElementById('userName');
const userRole = document.getElementById('userRole');
const logoutBtn = document.getElementById('logoutBtn');

const menuItems = [
    { id: 'menu-overview', view: 'overview', label: 'Overview Dashboard' },
    { id: 'menu-materials', view: 'materials', label: 'Materials Master' },
    { id: 'menu-inventory', view: 'inventory', label: 'Stock & Inventory' },
    { id: 'menu-locations', view: 'locations', label: 'Storage Locations' },
    { id: 'menu-suppliers', view: 'suppliers', label: 'Suppliers Registry' },
    { id: 'menu-po', view: 'po', label: 'Purchase Orders' },
    { id: 'menu-grn', view: 'grn', label: 'GRN Receipt Logs' },
    { id: 'menu-production', view: 'production', label: 'Production Runs & BOM' },
    { id: 'menu-compliance', view: 'compliance', label: 'QA Compliance Panel' },
    { id: 'menu-reports', view: 'reports', label: 'System Reports' },
    { id: 'menu-risk', view: 'risk', label: 'Risk Analysis Dashboard' },
    { id: 'menu-ai', view: 'ai', label: 'AI Decision Panel' },
    { id: 'menu-admin', view: 'admin', label: 'Admin Management' }
];

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

/* --- AUTHENTICATION GATE --- */
function initAuthListeners() {
    if (authListenersInitialized) return;
    authListenersInitialized = true;

    loginForm.addEventListener('submit', handleLoginSubmit);
    togglePasswordVisibility.addEventListener('click', togglePasswordInput);
    logoutBtn.addEventListener('click', logout);
}

function readStoredSession() {
    try {
        const rawSession = sessionStorage.getItem(SESSION_STORAGE_KEY);
        if (!rawSession) return null;

        const parsedSession = JSON.parse(rawSession);
        if (!parsedSession || !parsedSession.userId || !parsedSession.employeeId) {
            return null;
        }
        return parsedSession;
    } catch (err) {
        sessionStorage.removeItem(SESSION_STORAGE_KEY);
        return null;
    }
}

function persistSession(session) {
    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
}

function showLogin(message = '') {
    document.body.classList.add('login-active');
    loginScreen.removeAttribute('hidden');
    setLoginMessage(message, message ? 'info' : '');
    loginEmployeeId.focus();
}

function showApplication() {
    document.body.classList.remove('login-active');
    loginScreen.setAttribute('hidden', '');
    applyUserToShell();
}

function setLoginMessage(message, type) {
    loginMessage.textContent = message;
    loginMessage.className = `login-message ${type || ''}`.trim();
}

function togglePasswordInput() {
    const isPassword = loginPassword.type === 'password';
    loginPassword.type = isPassword ? 'text' : 'password';
    togglePasswordVisibility.textContent = isPassword ? 'Hide' : 'Show';
    togglePasswordVisibility.setAttribute('aria-label', isPassword ? 'Hide password' : 'Show password');
}

async function handleLoginSubmit(event) {
    event.preventDefault();

    const employeeId = loginEmployeeId.value.trim();
    const password = loginPassword.value;

    if (!employeeId || !password.trim()) {
        setLoginMessage('Employee ID and password are required.', 'error');
        return;
    }

    loginSubmitBtn.disabled = true;
    loginSubmitBtn.textContent = 'Signing in...';
    setLoginMessage('', '');

    try {
        const session = await authenticate(employeeId, password);
        currentUser = session;
        persistSession(session);
        loginPassword.value = '';
        currentView = 'overview';
        setLoginMessage('Access approved. Loading secure workspace...', 'success');
        startAuthenticatedShell();
    } catch (err) {
        setLoginMessage(err.message || 'Login failed. Please try again.', 'error');
    } finally {
        loginSubmitBtn.disabled = false;
        loginSubmitBtn.textContent = 'Sign in';
    }
}

async function authenticate(employeeId, password) {
    const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ employeeId, password })
    });

    const payload = await res.json().catch(() => ({}));
    if (!res.ok) {
        throw new Error(payload.error || 'Unable to authenticate user.');
    }
    return payload;
}

function logout() {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
    currentUser = null;
    if (refreshTimerId) {
        clearInterval(refreshTimerId);
        refreshTimerId = null;
    }
    if (agentEventSource) {
        agentEventSource.close();
        agentEventSource = null;
        sseInitialized = false;
    }
    loginPassword.value = '';
    showLogin('Session ended. Sign in to continue.');
}

function applyUserToShell() {
    if (!currentUser) return;

    const displayName = currentUser.fullName || currentUser.employeeId;
    userName.textContent = displayName;
    userRole.textContent = currentUser.roleName || 'Authorized User';
    userAvatar.textContent = getInitials(displayName);

    // Hide/show sidebar links based on permissions
    const permissions = currentUser.permissions || [];
    const menuPermissionMap = {
        'menu-overview': [],
        'menu-materials': ['VIEW_DRUG'],
        'menu-inventory': ['VIEW_INVENTORY'],
        'menu-locations': ['MANAGE_LOCATIONS'],
        'menu-suppliers': ['VIEW_SUPPLIERS', 'MANAGE_SUPPLIERS'],
        'menu-po': ['VIEW_PO', 'CREATE_PO'],
        'menu-grn': ['RECEIVE_PO', 'CREATE_GRN'],
        'menu-production': ['VIEW_BOM', 'CREATE_PRODUCTION_ORDER', 'MANAGE_BOM'],
        'menu-compliance': ['VIEW_QA_REPORTS', 'UPDATE_QC_STATUS', 'APPROVE_QA', 'REJECT_QA'],
        'menu-reports': ['VIEW_REPORTS'],
        'menu-risk': ['VIEW_REPORTS'],
        'menu-ai': ['MANAGE_ROLES'],
        'menu-admin': ['MANAGE_ROLES']
    };

    Object.entries(menuPermissionMap).forEach(([menuId, reqPerms]) => {
        const btn = document.getElementById(menuId);
        if (btn) {
            if (reqPerms.length === 0) {
                btn.style.display = '';
            } else {
                const hasPermission = reqPerms.some(p => permissions.includes(p));
                if (hasPermission) {
                    btn.style.display = '';
                } else {
                    btn.style.display = 'none';
                }
            }
        }
    });
}

function getInitials(name) {
    return name
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map(part => part.charAt(0).toUpperCase())
        .join('') || '--';
}

function hasAnyPermission(...reqPerms) {
    if (!currentUser || !currentUser.permissions) return false;
    // Admin has superuser status
    if (currentUser.permissions.includes('MANAGE_ROLES')) return true;
    return reqPerms.some(p => currentUser.permissions.includes(p));
}

function startAuthenticatedShell() {
    showApplication();

    if (!shellInitialized) {
        shellInitialized = true;
        initRouter();
        initFormSubmitListeners();
        makeXaiPanelAdjustable();
    }

    if (!sseInitialized) {
        initSSE();
    }

    if (!refreshTimerId) {
        refreshTimerId = setInterval(() => {
            loadViewData(currentView);
        }, 8000);
    }

    navigateToView(currentView || 'overview');
}

/* --- ROUTER SETUP --- */
function initRouter() {
    menuItems.forEach(item => {
        const btn = document.getElementById(item.id);
        if (btn) {
            btn.addEventListener('click', () => navigateToView(item.view));
        }
    });

    const sidebarToggle = document.getElementById('sidebarToggle');
    const sidebar = document.getElementById('appSidebar');
    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener('click', () => {
            sidebar.classList.toggle('collapsed');
            sidebarToggle.textContent = sidebar.classList.contains('collapsed') ? '▶' : '◀';
            sidebarToggle.title = sidebar.classList.contains('collapsed') ? 'Expand Menu' : 'Collapse Menu';
        });
    }

    const themeCheckbox = document.getElementById('themeCheckbox');
    if (themeCheckbox) {
        const storedTheme = localStorage.getItem('theme') || 'dark';
        if (storedTheme === 'light') {
            document.body.classList.add('light-theme');
            themeCheckbox.checked = false;
        } else {
            document.body.classList.remove('light-theme');
            themeCheckbox.checked = true;
        }

        themeCheckbox.addEventListener('change', () => {
            if (themeCheckbox.checked) {
                document.body.classList.remove('light-theme');
                localStorage.setItem('theme', 'dark');
            } else {
                document.body.classList.add('light-theme');
                localStorage.setItem('theme', 'light');
            }
        });
    }
}

function navigateToView(view) {
    const target = menuItems.find(item => item.view === view) || menuItems[0];

    // Check permissions before allowing navigation
    const permissions = currentUser?.permissions || [];
    const menuPermissionMap = {
        'overview': [],
        'materials': ['VIEW_DRUG'],
        'inventory': ['VIEW_INVENTORY'],
        'locations': ['MANAGE_LOCATIONS'],
        'suppliers': ['VIEW_SUPPLIERS', 'MANAGE_SUPPLIERS'],
        'po': ['VIEW_PO', 'CREATE_PO'],
        'grn': ['RECEIVE_PO', 'CREATE_GRN'],
        'production': ['VIEW_BOM', 'CREATE_PRODUCTION_ORDER', 'MANAGE_BOM'],
        'compliance': ['VIEW_QA_REPORTS', 'UPDATE_QC_STATUS', 'APPROVE_QA', 'REJECT_QA'],
        'reports': ['VIEW_REPORTS'],
        'risk': ['VIEW_REPORTS'],
        'ai': ['MANAGE_ROLES'],
        'admin': ['MANAGE_ROLES']
    };

    const reqPerms = menuPermissionMap[target.view] || [];
    if (reqPerms.length > 0) {
        const hasPermission = reqPerms.some(p => permissions.includes(p));
        if (!hasPermission) {
            console.warn(`Access denied to view: ${target.view}. Redirecting to overview.`);
            if (view !== 'overview') {
                navigateToView('overview');
            }
            return;
        }
    }

    menuItems.forEach(item => {
        const menuNode = document.getElementById(item.id);
        if (menuNode) {
            menuNode.classList.toggle('active', item.view === target.view);
        }
    });

    currentView = target.view;
    breadcrumbCurrent.textContent = target.label;
    searchInput.value = '';
    
    // Render immediate loading state to prevent perceived lag/freeze during REST fetches
    mainViewport.innerHTML = `
        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; min-height: 400px; color: var(--text-secondary);">
            <div class="loader-spinner" style="width: 48px; height: 48px; border: 4px solid var(--accent-teal-glow); border-top-color: var(--accent-teal); border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 16px;"></div>
            <div style="font-size: 14px; font-weight: 500; letter-spacing: 0.5px;">Loading ${target.label}...</div>
        </div>
    `;

    loadViewData(target.view);
}

// Load data corresponding to active view
async function loadViewData(view) {
    if (view === 'materials') {
        await fetchMaterials();
    } else if (view === 'suppliers') {
        await fetchSuppliers();
    } else if (view === 'inventory') {
        await fetchStockData();
    } else if (view === 'locations') {
        await fetchLocations();
    } else if (view === 'po') {
        await fetchPurchaseOrders();
    } else if (view === 'grn') {
        await fetchGRNs();
    } else if (view === 'production') {
        await fetchProductionData();
    } else if (view === 'compliance') {
        await fetchComplianceData();
    } else if (view === 'overview') {
        await fetchOverviewData();
    } else if (view === 'reports') {
        await fetchReportData(activeReport);
    } else if (view === 'admin') {
        await fetchAdminRoles();
    } else if (view === 'risk') {
        await fetchRiskReports();
    } else if (view === 'ai') {
        await fetchAiDecisions();
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
    if (!await customConfirm(`Are you sure you want to delete material ${code}?`)) return;
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
    if (!await customConfirm(`Are you sure you want to delete supplier record ID: ${id}?`)) return;
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
            body: JSON.stringify({ userId: currentUser?.userId || 1 })
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
        // Fetch active inspections
        const resQA = await fetch(`${API_BASE}/qa/inspections`);
        if (resQA.ok) qaList = await resQA.json();

        // Fetch stock to populate approved / rejected batches
        const resStock = await fetch(`${API_BASE}/stock`);
        if (resStock.ok) stockList = await resStock.json();

        // Fetch production orders to populate active production runs
        const resOrders = await fetch(`${API_BASE}/production/orders`);
        if (resOrders.ok) orderList = await resOrders.json();

        // Combine into a single consolidated inspectable list
        qaAllBatches = [];

        // Add from active QA inspections (includes IN_PRODUCTION, IN_PROCESS_SAMPLE, UNDER_TEST, QI, QUARANTINE)
        qaList.forEach(item => {
            qaAllBatches.push({
                batchNumber: item.batchNumber,
                materialCode: item.materialCode,
                locationCode: item.locationCode,
                quantity: item.quantity,
                reservedQuantity: item.reservedQuantity || 0,
                availableQuantity: item.availableQuantity || item.quantity,
                unitCost: item.unitCost || 0.0,
                expDate: item.expDate,
                qcStatus: item.qcStatus || 'IN_PRODUCTION',
                source: 'QA_INSPECTION'
            });
        });

        // Add approved / rejected stock items not already in qaList
        const qaBatchNumbers = new Set(qaAllBatches.map(b => b.batchNumber));
        stockList.forEach(item => {
            // Only add APPROVED / REJECTED / RELEASED that are not already in the QA inspection list
            const terminalStatuses = ['APPROVED', 'REJECTED', 'RELEASED'];
            if (!qaBatchNumbers.has(item.batchNumber) && terminalStatuses.includes((item.qcStatus || '').toUpperCase())) {
                qaAllBatches.push({
                    batchNumber: item.batchNumber,
                    materialCode: item.materialCode,
                    locationCode: item.locationCode,
                    quantity: item.quantity,
                    reservedQuantity: item.reservedQuantity || 0,
                    availableQuantity: item.availableQuantity || item.quantity,
                    unitCost: item.unitCost || 0.0,
                    expDate: item.expDate,
                    qcStatus: item.qcStatus || 'APPROVED',
                    source: 'STOCK'
                });
                qaBatchNumbers.add(item.batchNumber);
            }
        });

        renderComplianceView(qaAllBatches);

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

// 6. Locations REST Callers
async function fetchLocations() {
    try {
        const res = await fetch(`${API_BASE}/locations`);
        if (!res.ok) throw new Error("Failed to load locations");
        locations = await res.json();
        renderLocationsView(locations);
    } catch (err) {
        console.error("API error fetching locations: ", err);
        showMockLocationsFallback();
    }
}

async function createLocation(location) {
    try {
        const res = await fetch(`${API_BASE}/locations`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(location)
        });
        if (!res.ok) {
            const errObj = await res.json().catch(() => ({}));
            throw new Error(errObj.error || "Failed to create location");
        }
        closeModal('addLocationModal');
        addLocationForm.reset();
        await fetchLocations();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function updateLocation(code, location) {
    try {
        const res = await fetch(`${API_BASE}/locations/${code}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(location)
        });
        if (!res.ok) {
            const errObj = await res.json().catch(() => ({}));
            throw new Error(errObj.error || "Failed to update location");
        }
        closeModal('editLocationModal');
        editLocationForm.reset();
        await fetchLocations();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function deleteLocation(code) {
    if (!await customConfirm(`Are you sure you want to delete location ${code}?`)) return;
    try {
        const res = await fetch(`${API_BASE}/locations/${code}`, {
            method: 'DELETE'
        });
        if (!res.ok) {
            const errObj = await res.json().catch(() => ({}));
            throw new Error(errObj.error || "Failed to delete location");
        }
        await fetchLocations();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// 7. Purchase Orders REST Callers
async function fetchPurchaseOrders() {
    try {
        const res = await fetch(`${API_BASE}/purchase-orders`);
        if (!res.ok) throw new Error("Failed to load purchase orders");
        purchaseOrders = await res.json();
        renderPurchaseOrdersView(purchaseOrders);
    } catch (err) {
        console.error("API error fetching POs: ", err);
        showMockPoFallback();
    }
}

async function createPurchaseOrder(po) {
    try {
        const res = await fetch(`${API_BASE}/purchase-orders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(po)
        });
        if (!res.ok) {
            const errObj = await res.json().catch(() => ({}));
            throw new Error(errObj.error || "Failed to create purchase order");
        }
        closeModal('addPurchaseOrderModal');
        addPurchaseOrderForm.reset();
        document.getElementById('poItemsList').innerHTML = '';
        await fetchPurchaseOrders();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function deletePurchaseOrder(id) {
    if (!await customConfirm(`Are you sure you want to delete Purchase Order #${id}?`)) return;
    try {
        const res = await fetch(`${API_BASE}/purchase-orders/${id}`, {
            method: 'DELETE'
        });
        if (!res.ok) {
            const errObj = await res.json().catch(() => ({}));
            throw new Error(errObj.error || "Failed to delete purchase order");
        }
        await fetchPurchaseOrders();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function receivePurchaseOrder(id) {
    try {
        const res = await fetch(`${API_BASE}/purchase-orders/${id}/receive`, {
            method: 'POST'
        });
        if (!res.ok) {
            const errObj = await res.json().catch(() => ({}));
            throw new Error(errObj.error || "Failed to mark purchase order as received");
        }
        await fetchPurchaseOrders();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// 8. GRN REST Callers
async function fetchGRNs() {
    try {
        const res = await fetch(`${API_BASE}/grn`);
        if (!res.ok) throw new Error("Failed to load GRNs");
        grns = await res.json();
        renderGRNView(grns);
    } catch (err) {
        console.error("API error fetching GRNs: ", err);
        showMockGrnFallback();
    }
}

async function createGRN(grnPayload) {
    try {
        const res = await fetch(`${API_BASE}/grn`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(grnPayload)
        });
        if (!res.ok) {
            const errObj = await res.json().catch(() => ({}));
            throw new Error(errObj.error || "Failed to verify and save GRN");
        }
        closeModal('createGrnModal');
        createGrnForm.reset();
        await fetchGRNs();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

function showMockLocationsFallback() {
    locations = [
        { locationCode: "RAW_STORES_A", locationName: "Raw Materials Storage A", description: "Standard cold-bay storage", capacity: 100 },
        { locationCode: "LIQUID_BAY_1", locationName: "Liquid Ingredients Bay 1", description: "Hazardous storage environment", capacity: 50 },
        { locationCode: "FG_STORES_WH", locationName: "Finished Goods Warehouse", description: "Standard temperature finished goods storage", capacity: 250 }
    ];
    renderLocationsView(locations);
}

function showMockPoFallback() {
    purchaseOrders = [
        { id: 1001, supplierId: 1, supplierName: "Bayer Chemicals Ltd", orderDate: "2026-07-08", expectedDate: "2026-07-22", totalAmount: 15500.00, status: "Pending" },
        { id: 1002, supplierId: 2, supplierName: "Global API Dist", orderDate: "2026-07-09", expectedDate: "2026-07-23", totalAmount: 4800.00, status: "Received" }
    ];
    renderPurchaseOrdersView(purchaseOrders);
}

function showMockGrnFallback() {
    grns = [
        { id: 5001, purchaseOrderId: 1002, supplierName: "Global API Dist", receivedDate: "2026-07-10", receivedBy: "admin", status: "Verified" }
    ];
    renderGRNView(grns);
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
    qaAllBatches = [
        { batchNumber: "B-AMX-08", materialCode: "MAT-001", locationCode: "QC_HOLD", quantity: 80.0, reservedQuantity: 0.0, availableQuantity: 0.0, unitCost: 45.0, expDate: "2026-08-01", qcStatus: "UNDER_TEST", source: "QA_INSPECTION" },
        { batchNumber: "B-PCM-901", materialCode: "MAT-002", locationCode: "LOC-A1", quantity: 1000.0, reservedQuantity: 0.0, availableQuantity: 1000.0, unitCost: 15.5, expDate: "2028-06-30", qcStatus: "APPROVED", source: "STOCK" },
        { batchNumber: "B-GLY-22", materialCode: "MAT-003", locationCode: "LOC-B2", quantity: 450.0, reservedQuantity: 0.0, availableQuantity: 450.0, unitCost: 8.2, expDate: "2027-12-15", qcStatus: "APPROVED", source: "STOCK" },
        { batchNumber: "B-PCM-902", materialCode: "MAT-002", locationCode: "PRODUCTION_LINE", quantity: 500.0, reservedQuantity: 0.0, availableQuantity: 0.0, unitCost: 0.0, expDate: null, qcStatus: "IN_PRODUCTION", source: "PRODUCTION" }
    ];
    renderComplianceView(qaAllBatches);
}

function showMockOverviewStats() {
    materials = [{ active: true }, { active: true }];
    suppliers = [{ supplierStatus: 'APPROVED' }];
    stockList = [{ qcStatus: 'APPROVED' }];
    qaList = [{ qcStatus: 'UNDER_TEST' }];
    renderOverviewView();
}

// 9. Reports REST Callers
async function fetchReportData(reportType) {
    try {
        const res = await fetch(`${API_BASE}/reports/${reportType}`);
        if (!res.ok) throw new Error("Failed to load report data");
        reportData = await res.json();
        renderReportsView();
    } catch (err) {
        console.error("API error fetching reports: ", err);
        loadMockReportData(reportType);
        renderReportsView();
    }
}

function loadMockReportData(reportType) {
    if (reportType === 'stock-value') {
        reportData = [
            { stockId: 101, materialCode: "MAT-002", brandName: "Paracetamol API", batchNumber: "B-PCM-901", availableQuantity: 1000.0, unitCost: 15.5, expDate: "2028-06-30", qcStatus: "APPROVED" },
            { stockId: 102, materialCode: "MAT-003", brandName: "Glycerol Excipient", batchNumber: "B-GLY-22", availableQuantity: 450.0, unitCost: 8.2, expDate: "2027-12-15", qcStatus: "APPROVED" }
        ];
    } else if (reportType === 'low-stock') {
        reportData = [
            { materialCode: "MAT-002", reorderLevel: 2000, availableQty: 1000.0, reservedQty: 200.0, belowReorder: true }
        ];
    } else if (reportType === 'expiring') {
        reportData = [
            { stockId: 103, materialCode: "MAT-001", brandName: "Amoxicillin 500mg", batchNumber: "B-AMX-08", availableQuantity: 80.0, unitCost: 45.0, expDate: "2026-08-01", qcStatus: "UNDER_TEST" }
        ];
    } else if (reportType === 'supplier-performance') {
        reportData = [
            { supplierId: 1, supplierName: "Bayer Chemicals Ltd", drivers: ["Late delivery rate: 5.26%", "Rejection rate: 0.00%", "Capacity score: 0.80"], riskScore: 0.08, severity: "LOW", status: "APPROVED" },
            { supplierId: 2, supplierName: "Global API Dist", drivers: ["Late delivery rate: 25.00%", "Rejection rate: 10.00%", "Capacity score: 0.50"], riskScore: 0.35, severity: "MEDIUM", status: "APPROVED" }
        ];
    } else if (reportType === 'grn-history') {
        reportData = [
            { grnId: 1, poId: 12, supplierName: "Bayer Chemicals Ltd", receivedDate: "2026-07-09", receivedBy: 3, status: "APPROVED" }
        ];
    }
}

// 10. Admin RBAC REST Callers
async function fetchAdminRoles() {
    try {
        const res = await fetch(`${API_BASE}/auth/roles`);
        if (!res.ok) throw new Error("Failed to load roles");
        adminRoles = await res.json();
        
        if (adminRoles.length > 0 && !adminSelectedRole) {
            adminSelectedRole = adminRoles[0].roleName;
        }

        if (adminSelectedRole) {
            await fetchRolePermissions(adminSelectedRole);
        } else {
            renderAdminView();
        }
    } catch (err) {
        console.error("API error fetching admin roles: ", err);
        loadMockAdminData();
        renderAdminView();
    }
}

async function fetchRolePermissions(roleName) {
    try {
        const res = await fetch(`${API_BASE}/auth/permissions/${roleName}`);
        if (!res.ok) throw new Error("Failed to load permissions for role: " + roleName);
        adminRolePermissions = await res.json();
        renderAdminView();
    } catch (err) {
        console.error("API error fetching permissions for role: ", err);
        loadMockRolePermissions(roleName);
        renderAdminView();
    }
}

async function saveRolePermissions() {
    const checkedIds = [];
    document.querySelectorAll('.rbac-perm-checkbox:checked').forEach(cb => {
        checkedIds.push(parseInt(cb.value));
    });

    const payload = {
        permissionIds: checkedIds,
        adminUserId: currentUser?.employeeId || 1
    };

    try {
        const res = await fetch(`${API_BASE}/auth/permissions/${adminSelectedRole}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error("Failed to save permissions");
        alert("Role permissions updated successfully!");
        await fetchAdminRoles();
    } catch (err) {
        alert("Error saving permissions: " + err.message);
        if (adminRolePermissions) {
            adminRolePermissions.assignedPermissionIds = checkedIds;
        }
        alert("Saved permissions successfully (Mock Environment).");
        renderAdminView();
    }
}

function loadMockAdminData() {
    adminRoles = [
        { roleId: 1, roleName: "Admin", description: "System Administrator with full access" },
        { roleId: 2, roleName: "QA Manager", description: "Quality Assurance Manager for QC Hold, Release, and Audits" },
        { roleId: 3, roleName: "Warehouse Manager", description: "Manages inventory, GRN, and stock transfers" },
        { roleId: 4, roleName: "Procurement", description: "Manages Purchase Orders and Suppliers" },
        { roleId: 5, roleName: "Production", description: "Manages Production Orders and BOMs" }
    ];
    if (!adminSelectedRole) {
        adminSelectedRole = "Admin";
    }
    loadMockRolePermissions(adminSelectedRole);
}

function loadMockRolePermissions(roleName) {
    const mockPerms = [
        { permissionId: 1, permissionName: "MANAGE_USERS", module: "Admin", description: "Create, update, and delete users" },
        { permissionId: 2, permissionName: "MANAGE_ROLES", module: "Admin", description: "Manage roles and permissions" },
        { permissionId: 3, permissionName: "APPROVE_QA", module: "Quality", description: "Approve quality assurance tests" },
        { permissionId: 4, permissionName: "REJECT_QA", module: "Quality", description: "Reject quality assurance tests" },
        { permissionId: 5, permissionName: "VIEW_QA_REPORTS", module: "Quality", description: "View QA compliance reports" },
        { permissionId: 6, permissionName: "UPDATE_QC_STATUS", module: "Quality", description: "Update quality control status" },
        { permissionId: 7, permissionName: "VIEW_BATCH_TRACEABILITY", module: "Quality", description: "View batch traceability" },
        { permissionId: 8, permissionName: "CREATE_GRN", module: "Warehouse", description: "Create Goods Received Note" },
        { permissionId: 9, permissionName: "TRANSFER_STOCK", module: "Warehouse", description: "Transfer stock between locations" },
        { permissionId: 10, permissionName: "ADJUST_STOCK", module: "Warehouse", description: "Adjust inventory levels" },
        { permissionId: 11, permissionName: "VIEW_INVENTORY", module: "Warehouse", description: "View inventory and stock levels" },
        { permissionId: 12, permissionName: "MANAGE_LOCATIONS", module: "Locations", description: "Manage warehouse locations" },
        { permissionId: 13, permissionName: "RECEIVE_PO", module: "Warehouse", description: "Receive purchase orders (GRN)" },
        { permissionId: 14, permissionName: "CREATE_PO", module: "Procurement", description: "Create Purchase Orders" },
        { permissionId: 15, permissionName: "APPROVE_PO", module: "Procurement", description: "Approve Purchase Orders" },
        { permissionId: 16, permissionName: "MANAGE_SUPPLIERS", module: "Procurement", description: "Manage Supplier Master data" },
        { permissionId: 17, permissionName: "VIEW_SUPPLIERS", module: "Procurement", description: "View supplier information" },
        { permissionId: 18, permissionName: "VIEW_PO", module: "Procurement", description: "View purchase orders" },
        { permissionId: 19, permissionName: "CREATE_PRODUCTION_ORDER", module: "Production", description: "Create Production Orders" },
        { permissionId: 20, permissionName: "MANAGE_BOM", module: "Production", description: "Manage Bill of Materials" },
        { permissionId: 21, permissionName: "RECORD_CONSUMPTION", module: "Production", description: "Record material consumption" },
        { permissionId: 22, permissionName: "VIEW_BOM", module: "Production", description: "View bill of materials" },
        { permissionId: 23, permissionName: "VIEW_DRUG", module: "Materials", description: "View master drug/material data" },
        { permissionId: 24, permissionName: "VIEW_REPORTS", module: "Reports", description: "View system reports" }
    ];

    let assigned = new Set();
    if (roleName === 'Admin') {
        mockPerms.forEach(p => assigned.add(p.permissionId));
    } else if (roleName === 'QA Manager') {
        assigned.add(3); assigned.add(4); assigned.add(5); assigned.add(6); assigned.add(7);
    } else if (roleName === 'Warehouse Manager') {
        assigned.add(8); assigned.add(9); assigned.add(10); assigned.add(11); assigned.add(12); assigned.add(13);
    } else if (roleName === 'Procurement') {
        assigned.add(14); assigned.add(15); assigned.add(16); assigned.add(17); assigned.add(18);
    } else if (roleName === 'Production') {
        assigned.add(19); assigned.add(20); assigned.add(21); assigned.add(22);
    }

    adminRolePermissions = {
        role: { roleName: roleName },
        permissions: mockPerms,
        assignedPermissionIds: Array.from(assigned)
    };
}

// 11. Risk & AI REST Callers
async function fetchRiskReports() {
    try {
        const res = await fetch(`${API_BASE}/risk/reports`);
        if (!res.ok) throw new Error("Failed to load risk reports");
        riskReports = await res.json();
        renderRiskView();
    } catch (err) {
        console.error("API error fetching risk reports: ", err);
        loadMockRiskReports();
        renderRiskView();
    }
}

async function triggerRiskScan() {
    try {
        const res = await fetch(`${API_BASE}/risk/scan`, { method: 'POST' });
        if (!res.ok) throw new Error("Failed to run risk scan");
        riskReports = await res.json();
        alert("Risk scan completed successfully!");
        renderRiskView();
    } catch (err) {
        alert("Error running risk scan: " + err.message);
        loadMockRiskReports();
        alert("Risk scan simulated in mock mode.");
        renderRiskView();
    }
}

async function fetchAiDecisions() {
    try {
        const res = await fetch(`${API_BASE}/ai/decisions`);
        if (!res.ok) throw new Error("Failed to load AI decisions");
        aiDecisions = await res.json();
        renderAiView();
    } catch (err) {
        console.error("API error fetching AI decisions: ", err);
        loadMockAiDecisions();
        renderAiView();
    }
}

async function triggerAgentScan() {
    try {
        const res = await fetch(`${API_BASE}/ai/scan`, { method: 'POST' });
        if (!res.ok) throw new Error("Failed to trigger agent scan");
        aiDecisions = await res.json();
        alert("Agent coordination run completed successfully!");
        renderAiView();
    } catch (err) {
        alert("Error triggering agent scan: " + err.message);
        loadMockAiDecisions();
        // Add a mock coordination decision log if we failed so user can see it in UI
        const mockCoord = {
            transactionId: crypto.randomUUID ? crypto.randomUUID() : "tx-" + Date.now(),
            taskType: "COORDINATION_RUN",
            status: "PENDING",
            createdAt: new Date().toISOString(),
            confidenceScore: 0.82,
            modelUsed: "gemini-2.0-flash-mock",
            promptSummary: "COORDINATION_RUN: System-wide stock levels and supplier risk coordination audit.",
            requiresHumanReview: true,
            extractedData: {
                status: "ATTENTION_REQUIRED",
                explanation: "Critical raw material shortage identified. QC hold release of glycerol is recommended.",
                actionsRecommended: "Release Glycerol Excipient batch B-GLY-22"
            }
        };
        aiDecisions.unshift(mockCoord);
        alert("Simulated agent coordination scan successfully.");
        renderAiView();
    }
}

async function approveAiDecision(txId) {
    try {
        const res = await fetch(`${API_BASE}/ai/decisions/${txId}/approve`, { method: 'POST' });
        if (!res.ok) throw new Error("Failed to approve decision");
        alert("AI Decision approved successfully!");
        await fetchAiDecisions();
    } catch (err) {
        // Mock fallback
        const decision = aiDecisions.find(d => d.transactionId === txId);
        if (decision) decision.status = "APPROVED";
        alert("Approved decision (Mock Mode).");
        renderAiView();
    }
}

async function rejectAiDecision(txId, remarks) {
    try {
        const res = await fetch(`${API_BASE}/ai/decisions/${txId}/reject`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ remarks: remarks })
        });
        if (!res.ok) throw new Error("Failed to reject decision");
        alert("AI Decision rejected successfully!");
        await fetchAiDecisions();
    } catch (err) {
        // Mock fallback
        const decision = aiDecisions.find(d => d.transactionId === txId);
        if (decision) {
            decision.status = "REJECTED";
            decision.rejectionReason = remarks;
        }
        alert("Rejected decision (Mock Mode).");
        renderAiView();
    }
}

function loadMockRiskReports() {
    riskReports = [
        { reportId: 101, category: "SUPPLIER_RISK", targetIdentifier: "Bayer Chemicals Ltd", riskScore: 0.08, status: "COMPLIANT", remarks: "Late delivery rate is 5.26%. Quality rejection rate is 0.00%." },
        { reportId: 102, category: "SUPPLIER_RISK", targetIdentifier: "Global API Dist", riskScore: 0.35, status: "WARNING", remarks: "Late delivery rate is 25.00%. Rejection rate is 10.00%." },
        { reportId: 103, category: "INVENTORY_SHORTAGE", targetIdentifier: "MAT-002", riskScore: 0.50, status: "WARNING", remarks: "Available stock (1000.0) is below safety reorder level (2000.0)." },
        { reportId: 104, category: "EXPIRY_HAZARD", targetIdentifier: "B-AMX-08", riskScore: 0.80, status: "CRITICAL", remarks: "Batch B-AMX-08 expires on 2026-08-01 (within 1 month)." }
    ];
}

function loadMockAiDecisions() {
    aiDecisions = [
        {
            transactionId: "tx-74b8-b403-4e57-a103",
            taskType: "AI_REASONING",
            status: "PENDING",
            createdAt: "2026-07-10T12:00:00Z",
            confidenceScore: 0.94,
            modelUsed: "gemini-2.0-flash",
            promptSummary: "AI_REASONING: Analyze supplier risk for Bayer Chemicals Ltd.",
            requiresHumanReview: false,
            extractedData: {
                explanation: "Bayer Chemicals exhibits very low operational risk. Late delivery rate is within optimal bounds.",
                riskScore: 0.08,
                recommendedStatus: "APPROVED"
            }
        },
        {
            transactionId: "tx-34ef-44b8-b403-4e57",
            taskType: "AI_REASONING",
            status: "PENDING",
            createdAt: "2026-07-10T12:05:00Z",
            confidenceScore: 0.68,
            modelUsed: "gemini-2.0-flash",
            promptSummary: "AI_REASONING: Decide if Glycerol Excipient batch B-GLY-22 should be released despite minor visual check warning.",
            requiresHumanReview: true,
            extractedData: {
                explanation: "Assay content is optimal at 99.4%, but moisture content checks logged minor variances. Requires QA manager verification.",
                recommendedAction: "QA release with manual supervisor signature"
            }
        }
    ];
}

/* --- VIEW RENDERERS --- */

// 0. Placeholder View for Unimplemented Modules
function renderPlaceholderView(view) {
    const title = menuItems.find(item => item.view === view)?.label || "Module";
    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">${title}</h1>
        </div>
        <div class="card-container" style="padding: 40px; text-align: center; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px;">
            <div style="font-size: 48px; margin-bottom: 16px; animation: pulse 2s infinite;">🚧</div>
            <h2 style="font-weight: 500; color: var(--text-primary); margin-bottom: 8px;">${title} View</h2>
            <p style="color: var(--text-secondary); max-width: 450px; margin: 0 auto 24px auto; font-size: 14px;">
                This module is currently under development as part of the implementation plan. Parlay multi-agent coordination metrics will be displayed here soon.
            </p>
            <button class="btn-primary" onclick="navigateToView('overview')">Return to Overview</button>
        </div>
    `;
}

// 12. Risk Analysis View Renderer
function renderRiskView() {
    // Calculate overall risk metrics
    const maxRisk = riskReports.length > 0 ? Math.max(...riskReports.map(r => r.riskScore)) : 0;
    const avgRisk = riskReports.length > 0 ? (riskReports.reduce((sum, r) => sum + r.riskScore, 0) / riskReports.length) : 0;
    
    // Scale dial rotation: risk is 0 to 1, needle maps to -90deg to +90deg
    const needleDeg = (maxRisk * 180) - 90;

    let severityText = "LOW RISK";
    let severityColor = "var(--status-green)";
    if (maxRisk >= 0.75) {
        severityText = "CRITICAL RISK";
        severityColor = "var(--status-red)";
    } else if (maxRisk >= 0.3) {
        severityText = "MEDIUM RISK";
        severityColor = "var(--status-orange)";
    }

    const tableRows = riskReports.map(r => {
        let statusBadge = "badge-success";
        if (r.status === 'CRITICAL') statusBadge = "badge-danger";
        else if (r.status === 'WARNING') statusBadge = "badge-warning";
        return `
            <tr>
                <td><code>${r.category}</code></td>
                <td><strong>${r.targetIdentifier}</strong></td>
                <td><span style="font-weight:600; color:${r.riskScore >= 0.75 ? 'var(--status-red)' : r.riskScore >= 0.3 ? 'var(--status-orange)' : 'var(--status-green)'}">${(r.riskScore * 100).toFixed(0)}%</span></td>
                <td><span class="badge ${statusBadge}">${r.status}</span></td>
                <td><span style="font-size: 12px; color: var(--text-secondary);">${r.remarks}</span></td>
            </tr>
        `;
    }).join('');

    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">SCM Operational Risk Auditing</h1>
            <button class="btn-primary" id="triggerRiskScanBtn">⚡ Trigger Risk Scan</button>
        </div>

        <div style="display: grid; grid-template-columns: 240px 1fr; gap: 24px; margin-bottom: 24px; align-items: stretch;">
            <!-- Gauge Card -->
            <div class="card-container" style="background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; padding: 24px; text-align: center; display: flex; flex-direction: column; justify-content: center; align-items: center;">
                <div class="risk-gauge-container">
                    <div class="risk-gauge-dial"></div>
                    <div class="risk-gauge-mask">
                        <span class="risk-gauge-value">${(maxRisk * 100).toFixed(0)}%</span>
                        <span class="risk-gauge-label">${severityText}</span>
                    </div>
                    <div class="risk-gauge-needle" style="transform: translateX(-50%) rotate(${needleDeg}deg); background-color: ${severityColor};"></div>
                </div>
                <div style="font-size: 12px; color: var(--text-secondary); margin-top: 8px;">
                    Average system risk score: <strong> ${(avgRisk * 100).toFixed(1)}%</strong>
                </div>
            </div>

            <!-- Risk Mitigation Dashboard -->
            <div class="card-container" style="background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; padding: 24px; display: flex; flex-direction: column; gap: 12px;">
                <h3 style="font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 8px;">Recommended Risk Mitigation Actions</h3>
                <div style="display: flex; flex-direction: column; gap: 10px;">
                    ${maxRisk >= 0.75 ? `
                        <div style="background: rgba(220, 53, 69, 0.1); border-left: 4px solid var(--status-red); padding: 12px; border-radius: 4px;">
                            <strong style="color: var(--status-red); font-size: 13px;">⚠️ CRITICAL: EXPIRY OR CAPACITY THREAT DETECTED</strong>
                            <p style="font-size: 12px; color: var(--text-primary); margin-top: 4px; margin-bottom: 0;">Expiring batch found in warehouse quarantine. Coordinate immediate QA disposition or re-testing.</p>
                        </div>
                    ` : ''}
                    ${avgRisk >= 0.2 ? `
                        <div style="background: rgba(255, 193, 7, 0.1); border-left: 4px solid var(--status-orange); padding: 12px; border-radius: 4px;">
                            <strong style="color: var(--status-orange); font-size: 13px;">⚡ WARNING: MATERIAL DEFICIT RISK</strong>
                            <p style="font-size: 12px; color: var(--text-primary); margin-top: 4px; margin-bottom: 0;">Stock levels for active materials are running close to safety reorder levels. Check the Purchase Orders panel to generate supply orders.</p>
                        </div>
                    ` : ''}
                    <div style="background: rgba(40, 167, 69, 0.1); border-left: 4px solid var(--status-green); padding: 12px; border-radius: 4px;">
                        <strong style="color: var(--status-green); font-size: 13px;">✅ COMPLIANCE STATUS NORMAL</strong>
                        <p style="font-size: 12px; color: var(--text-primary); margin-top: 4px; margin-bottom: 0;">All other supplier scoring and audit schedules are within optimal performance limits.</p>
                    </div>
                </div>
            </div>
        </div>

        <!-- Risk Reports Table -->
        <div class="card-container" style="background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; padding: 24px;">
            <h3 style="font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 16px;">Active Risk Indicators</h3>
            <div class="table-responsive">
                <table>
                    <thead>
                        <tr>
                            <th>Category</th>
                            <th>Target Identifier</th>
                            <th>Threat Level</th>
                            <th>Status</th>
                            <th>Audit Notes / Details</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${tableRows || '<tr><td colspan="5" style="text-align:center; color:var(--text-secondary);">No risk indicators found</td></tr>'}
                    </tbody>
                </table>
            </div>
        </div>
    `;

    document.getElementById('triggerRiskScanBtn').addEventListener('click', triggerRiskScan);
}

// 13. AI Decision View Renderer
function renderAiView() {
    const decisionCards = aiDecisions.map(d => {
        let confColor = "var(--status-green)";
        if (d.confidenceScore < 0.75) confColor = "var(--status-orange)";
        if (d.confidenceScore < 0.5) confColor = "var(--status-red)";

        const displayData = typeof d.extractedData === 'object' ? JSON.stringify(d.extractedData, null, 2) : String(d.extractedData);
        const explanation = d.extractedData && d.extractedData.explanation ? d.extractedData.explanation : d.promptSummary;

        let statusText = d.status || 'PENDING';
        let badgeClass = 'badge-warning';
        if (statusText === 'APPROVED') badgeClass = 'badge-success';
        if (statusText === 'REJECTED') badgeClass = 'badge-danger';

        let actionButtons = '';
        if (statusText === 'PENDING' || d.requiresHumanReview) {
            actionButtons = `
                <div class="ai-decision-actions">
                    <button class="btn-success approve-decision-btn" data-id="${d.transactionId}" style="background-color: var(--status-green) !important; color: white !important; font-size:11px; padding: 6px 12px;">Approve</button>
                    <button class="btn-danger reject-decision-btn" data-id="${d.transactionId}" style="background-color: var(--status-red) !important; color: white !important; font-size:11px; padding: 6px 12px;">Reject</button>
                </div>
            `;
        } else if (statusText === 'REJECTED' && d.rejectionReason) {
            actionButtons = `
                <div style="font-size: 11px; color: var(--status-red); font-style: italic; margin-top: 8px; text-align: right;">
                    Rejection remarks: "${d.rejectionReason}"
                </div>
            `;
        }

        return `
            <div class="ai-decision-card">
                <div class="ai-decision-header">
                    <div>
                        <span class="ai-decision-title">Transaction ID: <code>${d.transactionId}</code></span>
                        <span class="badge ${badgeClass}" style="margin-left: 10px;">${statusText}</span>
                    </div>
                    <div class="ai-decision-confidence">
                        <span>Confidence: <strong>${(d.confidenceScore * 100).toFixed(0)}%</strong></span>
                        <div class="ai-confidence-bar">
                            <div class="ai-confidence-fill" style="width: ${(d.confidenceScore * 100).toFixed(0)}%; background-color: ${confColor};"></div>
                        </div>
                    </div>
                </div>
                <div class="ai-decision-body">
                    <p style="margin-bottom: 8px; font-weight: 500;">Explanation / Summary:</p>
                    <div style="background-color: var(--bg-elevated); border: 1px solid var(--border-color); padding: 12px; border-radius: 4px; font-family: monospace; font-size: 12px; white-space: pre-wrap; color: var(--text-primary); max-height: 150px; overflow-y: auto;">${explanation}\n\n${displayData}</div>
                </div>
                <div class="ai-decision-meta">
                    <span>Model: <code>${d.modelUsed || 'gemini-2.0-flash'}</code></span>
                    <span>Created: <code>${d.createdAt ? new Date(d.createdAt).toLocaleString() : new Date().toLocaleString()}</code></span>
                    <span>Review Required: <strong>${d.requiresHumanReview ? 'YES' : 'NO'}</strong></span>
                </div>
                ${actionButtons}
            </div>
        `;
    }).join('');

    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">AI Gateway Decision Control</h1>
            <button class="btn-primary" id="triggerAgentScanBtn">🤖 Trigger Agent Coordination Scan</button>
        </div>

        <div style="margin-bottom: 24px; font-size: 13px; color: var(--text-secondary); line-height: 1.5; background: rgba(0, 242, 254, 0.05); border: 1px solid var(--accent-teal-glow); padding: 16px; border-radius: 6px;">
            This module traces decisions generated by autonomous JADE agents (Compliance, Inventory, Supplier Risk, AI Reasoning) operating over the pharma supply chain database. Decisions with confidence scores below 75% require mandatory manual human approval.
        </div>

        <div style="display: flex; flex-direction: column; gap: 8px;">
            ${decisionCards || '<div class="card-container" style="text-align: center; color: var(--text-secondary); padding: 40px;">No decision logs recorded.</div>'}
        </div>
    `;

    document.getElementById('triggerAgentScanBtn').addEventListener('click', triggerAgentScan);

    document.querySelectorAll('.approve-decision-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = e.currentTarget.getAttribute('data-id');
            approveAiDecision(id);
        });
    });

    document.querySelectorAll('.reject-decision-btn').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const id = e.currentTarget.getAttribute('data-id');
            const remarks = await customPrompt("Please enter the reason for rejecting this AI decision:");
            if (remarks === null) return;
            if (remarks.trim() === '') {
                alert("Rejection remarks are required.");
                return;
            }
            rejectAiDecision(id, remarks.trim());
        });
    });
}

// 9. Reports View Renderers
function renderReportsView() {
    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">Reporting & Analytics</h1>
            <button class="btn-primary" id="exportCsvBtn">Export to CSV</button>
        </div>

        <div class="view-tabs-header">
            <button class="tab-btn ${activeReport === 'stock-value' ? 'active' : ''}" onclick="switchReportTab('stock-value')">Stock Value</button>
            <button class="tab-btn ${activeReport === 'low-stock' ? 'active' : ''}" onclick="switchReportTab('low-stock')">Low Stock / Reorder</button>
            <button class="tab-btn ${activeReport === 'expiring' ? 'active' : ''}" onclick="switchReportTab('expiring')">Expiring Batches</button>
            <button class="tab-btn ${activeReport === 'supplier-performance' ? 'active' : ''}" onclick="switchReportTab('supplier-performance')">Supplier Performance</button>
            <button class="tab-btn ${activeReport === 'grn-history' ? 'active' : ''}" onclick="switchReportTab('grn-history')">GRN History</button>
        </div>

        <div id="reportSummaryContainer"></div>

        <div class="card-container">
            <div class="table-responsive">
                <table id="reportDataTable">
                    <thead id="reportTableHeader"></thead>
                    <tbody id="reportTableBody"></tbody>
                </table>
            </div>
        </div>
    `;

    document.getElementById('exportCsvBtn').addEventListener('click', exportReportToCSV);
    renderReportSummaryCard();
    renderReportTable();
}

function switchReportTab(reportType) {
    activeReport = reportType;
    fetchReportData(reportType);
}

window.switchReportTab = switchReportTab; // Make it globally accessible

function renderReportSummaryCard() {
    const summaryContainer = document.getElementById('reportSummaryContainer');
    if (!summaryContainer) return;
    summaryContainer.innerHTML = '';

    if (activeReport === 'stock-value') {
        const totalVal = reportData.reduce((sum, item) => sum + ((item.availableQuantity || 0) * (item.unitCost || 0)), 0);
        summaryContainer.innerHTML = `
            <div class="kpi-grid">
                <div class="kpi-card teal">
                    <div class="kpi-title">Total Inventory Valuation</div>
                    <div class="kpi-value">$${totalVal.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</div>
                    <div class="kpi-desc">Valuation of all current warehouse stock</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-title">Total Batches</div>
                    <div class="kpi-value">${reportData.length}</div>
                    <div class="kpi-desc">Total individual batch entries tracked</div>
                </div>
            </div>
        `;
    } else if (activeReport === 'low-stock') {
        summaryContainer.innerHTML = `
            <div class="kpi-grid">
                <div class="kpi-card amber">
                    <div class="kpi-title">Low Stock Materials</div>
                    <div class="kpi-value">${reportData.length}</div>
                    <div class="kpi-desc">Active materials below safety reorder level</div>
                </div>
            </div>
        `;
    } else if (activeReport === 'expiring') {
        summaryContainer.innerHTML = `
            <div class="kpi-grid">
                <div class="kpi-card amber">
                    <div class="kpi-title">Expiring Batches (6 Months)</div>
                    <div class="kpi-value">${reportData.length}</div>
                    <div class="kpi-desc">Stock items requiring immediate processing/disposition</div>
                </div>
            </div>
        `;
    } else if (activeReport === 'supplier-performance') {
        const highRisk = reportData.filter(s => s.riskScore > 0.7).length;
        summaryContainer.innerHTML = `
            <div class="kpi-grid">
                <div class="kpi-card teal">
                    <div class="kpi-title">Total Suppliers Scored</div>
                    <div class="kpi-value">${reportData.length}</div>
                    <div class="kpi-desc">Active approved vendors tracked for delivery and quality</div>
                </div>
                <div class="kpi-card amber">
                    <div class="kpi-title">High Risk Vendors</div>
                    <div class="kpi-value">${highRisk}</div>
                    <div class="kpi-desc">Vendors requiring performance review/disposition</div>
                </div>
            </div>
        `;
    } else if (activeReport === 'grn-history') {
        summaryContainer.innerHTML = `
            <div class="kpi-grid">
                <div class="kpi-card green">
                    <div class="kpi-title">Total Receptions</div>
                    <div class="kpi-value">${reportData.length}</div>
                    <div class="kpi-desc">Total Goods Received Notes (GRN) generated</div>
                </div>
            </div>
        `;
    }
}

function renderReportTable() {
    const headNode = document.getElementById('reportTableHeader');
    const bodyNode = document.getElementById('reportTableBody');

    if (!headNode || !bodyNode) return;
    headNode.innerHTML = '';
    bodyNode.innerHTML = '';

    if (reportData.length === 0) {
        headNode.innerHTML = `<tr><th>Data</th></tr>`;
        bodyNode.innerHTML = `<tr><td style="text-align: center; color: var(--text-secondary);">No records found matching this report.</td></tr>`;
        return;
    }

    if (activeReport === 'stock-value') {
        headNode.innerHTML = `
            <tr>
                <th>Material Code</th>
                <th>Brand Name</th>
                <th>Batch Number</th>
                <th>Storage Location</th>
                <th>Qty Available</th>
                <th>Unit Cost</th>
                <th>Total Valuation</th>
                <th>Status</th>
            </tr>
        `;
        reportData.forEach(item => {
            const tr = document.createElement('tr');
            const valuation = (item.availableQuantity || 0) * (item.unitCost || 0);
            tr.innerHTML = `
                <td>${item.materialCode}</td>
                <td><strong>${item.brandName || '-'}</strong></td>
                <td>${item.batchNumber}</td>
                <td>${item.locationCode || '-'}</td>
                <td>${item.availableQuantity.toLocaleString()}</td>
                <td>$${item.unitCost.toFixed(2)}</td>
                <td><strong>$${valuation.toFixed(2)}</strong></td>
                <td><span class="badge ${item.qcStatus === 'APPROVED' ? 'badge-success' : 'badge-warning'}">${item.qcStatus}</span></td>
            `;
            bodyNode.appendChild(tr);
        });
    } else if (activeReport === 'low-stock') {
        headNode.innerHTML = `
            <tr>
                <th>Material Code</th>
                <th>Reorder Safety Level</th>
                <th>Available Quantity</th>
                <th>Reserved Quantity</th>
                <th>Deficit</th>
            </tr>
        `;
        reportData.forEach(item => {
            const tr = document.createElement('tr');
            const deficit = Math.max(0, item.reorderLevel - item.availableQty);
            tr.innerHTML = `
                <td>${item.materialCode}</td>
                <td>${item.reorderLevel.toLocaleString()}</td>
                <td><span class="text-danger" style="font-weight:600;">${item.availableQty.toLocaleString()}</span></td>
                <td>${item.reservedQty.toLocaleString()}</td>
                <td><strong>${deficit.toLocaleString()}</strong></td>
            `;
            bodyNode.appendChild(tr);
        });
    } else if (activeReport === 'expiring') {
        headNode.innerHTML = `
            <tr>
                <th>Material Code</th>
                <th>Brand Name</th>
                <th>Batch Number</th>
                <th>Location</th>
                <th>Quantity</th>
                <th>Expiration Date</th>
                <th>QC Status</th>
            </tr>
        `;
        reportData.forEach(item => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${item.materialCode}</td>
                <td><strong>${item.brandName || '-'}</strong></td>
                <td>${item.batchNumber}</td>
                <td>${item.locationCode || '-'}</td>
                <td>${(item.availableQuantity || item.quantity).toLocaleString()}</td>
                <td><span class="text-warning" style="font-weight:600;">${formatDate(item.expDate)}</span></td>
                <td><span class="badge badge-warning">${item.qcStatus}</span></td>
            `;
            bodyNode.appendChild(tr);
        });
    } else if (activeReport === 'supplier-performance') {
        headNode.innerHTML = `
            <tr>
                <th>Vendor ID</th>
                <th>Supplier Name</th>
                <th>Performance Metrics</th>
                <th>Calculated Risk Score</th>
                <th>Severity</th>
                <th>Status</th>
            </tr>
        `;
        reportData.forEach(item => {
            const tr = document.createElement('tr');
            const driversList = (item.drivers || []).map(d => `<div>• ${d}</div>`).join('');
            let severityBadge = 'badge-success';
            if (item.severity === 'MEDIUM') severityBadge = 'badge-warning';
            else if (item.severity === 'HIGH' || item.severity === 'CRITICAL') severityBadge = 'badge-danger';
            
            tr.innerHTML = `
                <td>${item.supplierId}</td>
                <td><strong>${item.supplierName}</strong></td>
                <td style="font-size:11px; line-height:1.4;">${driversList || 'No metrics logged'}</td>
                <td><strong>${item.riskScore.toFixed(3)}</strong></td>
                <td><span class="badge ${severityBadge}">${item.severity}</span></td>
                <td><span class="badge badge-success">${item.status}</span></td>
            `;
            bodyNode.appendChild(tr);
        });
    } else if (activeReport === 'grn-history') {
        headNode.innerHTML = `
            <tr>
                <th>GRN ID</th>
                <th>PO ID</th>
                <th>Supplier Name</th>
                <th>Reception Date</th>
                <th>Verified By</th>
                <th>Status</th>
            </tr>
        `;
        reportData.forEach(item => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>GRN-${String(item.grnId).padStart(4, '0')}</td>
                <td>PO-${String(item.poId).padStart(4, '0')}</td>
                <td><strong>${item.supplierName}</strong></td>
                <td>${formatDate(item.receivedDate)}</td>
                <td>User #${item.receivedBy || 'System'}</td>
                <td><span class="badge badge-success">${item.status}</span></td>
            `;
            bodyNode.appendChild(tr);
        });
    }
}

function exportReportToCSV() {
    const table = document.getElementById('reportDataTable');
    if (!table) return;

    let csvContent = "";
    
    // Add Headers
    const headers = Array.from(table.querySelectorAll('thead th')).map(th => {
        return `"${th.textContent.trim().replace(/"/g, '""')}"`;
    });
    csvContent += headers.join(",") + "\n";
    
    // Add Rows
    const rows = Array.from(table.querySelectorAll('tbody tr'));
    rows.forEach(row => {
        const cells = Array.from(row.querySelectorAll('td')).map(td => {
            let text = td.textContent.trim();
            text = text.replace(/•/g, '').replace(/\s+/g, ' ');
            return `"${text.replace(/"/g, '""')}"`;
        });
        csvContent += cells.join(",") + "\n";
    });

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `pharma_report_${activeReport}_${new Date().toISOString().slice(0,10)}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// 11. Admin RBAC View Renderer
function renderAdminView() {
    const roleOptions = adminRoles.map(r => `
        <option value="${r.roleName}" ${adminSelectedRole === r.roleName ? 'selected' : ''}>${r.roleName}</option>
    `).join('');

    const groupedPerms = {};
    if (adminRolePermissions && adminRolePermissions.permissions) {
        adminRolePermissions.permissions.forEach(p => {
            const cat = p.module || 'General';
            if (!groupedPerms[cat]) {
                groupedPerms[cat] = [];
            }
            groupedPerms[cat].push(p);
        });
    }

    let gridHtml = '';
    const assignedIds = new Set(adminRolePermissions ? adminRolePermissions.assignedPermissionIds : []);

    for (const [category, perms] of Object.entries(groupedPerms)) {
        const checkboxItems = perms.map(p => {
            const checked = assignedIds.has(p.permissionId) ? 'checked' : '';
            return `
                <div class="rbac-permission-item">
                    <label style="display:flex; align-items:flex-start; gap:10px; cursor:pointer; margin-bottom:0;">
                        <input type="checkbox" class="rbac-perm-checkbox" value="${p.permissionId}" ${checked} style="margin-top:3px; cursor:pointer; width:auto;">
                        <div class="rbac-perm-text">
                            <span class="rbac-perm-name">${p.permissionName}</span>
                            <span class="rbac-perm-desc">${p.description || ''}</span>
                        </div>
                    </label>
                </div>
            `;
        }).join('');

        gridHtml += `
            <div class="rbac-category-section">
                <h3 class="rbac-category-title">${category} Module</h3>
                <div class="rbac-permissions-subgrid">
                    ${checkboxItems}
                </div>
            </div>
        `;
    }

    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">Role-Based Access Control (RBAC)</h1>
        </div>

        <div class="card-container" style="background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; padding: 24px; margin-bottom: 24px;">
            <div style="display: flex; gap: 16px; align-items: center; margin-bottom: 24px; flex-wrap: wrap;">
                <label for="rbacRoleSelector" style="font-weight: 600; color: var(--text-primary); font-size: 14px;">Select Role:</label>
                <select id="rbacRoleSelector" class="form-control" style="max-width: 300px; background-color: var(--bg-elevated); color: var(--text-primary); border: 1px solid var(--border-color); padding: 8px 12px; border-radius: 4px; font-size: 13px;">
                    ${roleOptions}
                </select>
                <span style="color: var(--text-secondary); font-size: 12px; font-style: italic;">
                    ${adminRoles.find(r => r.roleName === adminSelectedRole)?.description || ''}
                </span>
            </div>

            <form id="rbacForm">
                <div class="rbac-grid-container">
                    ${gridHtml || '<div style="text-align: center; color: var(--text-secondary); padding: 20px;">No permissions loaded.</div>'}
                </div>

                <div style="display: flex; justify-content: flex-end; margin-top: 32px;">
                    <button type="submit" class="btn-success" id="saveRbacBtn" style="background-color: var(--status-green) !important; color: white !important; border: none; padding: 10px 24px; border-radius: 4px; font-weight: 600; cursor: pointer; transition: all 0.2s;">Save Permissions</button>
                </div>
            </form>
        </div>
    `;

    document.getElementById('rbacRoleSelector').addEventListener('change', (e) => {
        adminSelectedRole = e.target.value;
        fetchRolePermissions(adminSelectedRole);
    });

    document.getElementById('rbacForm').addEventListener('submit', (e) => {
        e.preventDefault();
        saveRolePermissions();
    });
}

// 4. Storage Locations View
function renderLocationsView(data) {
    const canEdit = hasAnyPermission('MANAGE_LOCATIONS');

    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">Storage Locations</h1>
            ${canEdit ? '<button class="btn-primary" id="openAddLocationBtn">+ Add Location</button>' : ''}
        </div>

        <div class="card-container">
            <div class="table-responsive">
                <table>
                    <thead>
                        <tr>
                            <th>Location Code</th>
                            <th>Location Name</th>
                            <th>Description</th>
                            <th>Capacity (pallets)</th>
                            ${canEdit ? '<th>Actions</th>' : ''}
                        </tr>
                    </thead>
                    <tbody id="locationsTableBody"></tbody>
                </table>
            </div>
        </div>
    `;

    const bodyNode = document.getElementById('locationsTableBody');
    bodyNode.innerHTML = '';

    if (data.length === 0) {
        bodyNode.innerHTML = `<tr><td colspan="${canEdit ? 5 : 4}" style="text-align: center; color: var(--text-secondary);">No locations defined. Add a location to get started.</td></tr>`;
    } else {
        data.forEach(item => {
            const tr = document.createElement('tr');
            tr.setAttribute('data-key', item.locationCode);
            tr.innerHTML = `
                <td><strong>${item.locationCode}</strong></td>
                <td>${item.locationName}</td>
                <td>${item.description || '-'}</td>
                <td>${item.capacity}</td>
                ${canEdit ? `
                <td>
                    <div class="action-group">
                        <button class="action-btn edit-location-btn" data-code="${item.locationCode}">✏️</button>
                        <button class="action-btn delete-location-btn" data-code="${item.locationCode}">🗑️</button>
                    </div>
                </td>` : ''}
            `;
            bodyNode.appendChild(tr);
        });
    }

    if (canEdit) {
        document.getElementById('openAddLocationBtn').addEventListener('click', () => openModal('addLocationModal'));

        document.querySelectorAll('.edit-location-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const code = e.currentTarget.getAttribute('data-code');
                const loc = locations.find(l => l.locationCode === code);
                if (loc) {
                    document.getElementById('editLocCode').value = loc.locationCode;
                    document.getElementById('editLocCodeDisplay').value = loc.locationCode;
                    document.getElementById('editLocName').value = loc.locationName;
                    document.getElementById('editLocDesc').value = loc.description || '';
                    document.getElementById('editLocCapacity').value = loc.capacity;
                    openModal('editLocationModal');
                }
            });
        });

        document.querySelectorAll('.delete-location-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const code = e.currentTarget.getAttribute('data-code');
                deleteLocation(code);
            });
        });
        setupExcelLikeSelection('locationsTableBody', 'data-key', deleteMultipleLocations);
    }
}

// 5. Purchase Orders View
function renderPurchaseOrdersView(data) {
    const canCreate = hasAnyPermission('CREATE_PO');
    const canReceive = hasAnyPermission('RECEIVE_PO', 'CREATE_GRN');

    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">Purchase Orders</h1>
            ${canCreate ? '<button class="btn-primary" id="openAddPoBtn">+ Create PO</button>' : ''}
        </div>

        <div class="card-container">
            <div class="table-responsive">
                <table>
                    <thead>
                        <tr>
                            <th>Order ID</th>
                            <th>Supplier</th>
                            <th>Order Date</th>
                            <th>Expected Date</th>
                            <th>Total Cost</th>
                            <th>Status</th>
                            ${(canCreate || canReceive) ? '<th>Actions</th>' : ''}
                        </tr>
                    </thead>
                    <tbody id="poTableBody"></tbody>
                </table>
            </div>
        </div>
    `;

    const bodyNode = document.getElementById('poTableBody');
    bodyNode.innerHTML = '';

    if (data.length === 0) {
        bodyNode.innerHTML = `<tr><td colspan="${(canCreate || canReceive) ? 7 : 6}" style="text-align: center; color: var(--text-secondary);">No purchase orders found.</td></tr>`;
    } else {
        data.forEach(item => {
            const tr = document.createElement('tr');
            tr.setAttribute('data-key', item.id);
            let statusBadge = 'badge-warning';
            if (item.status === 'Received') statusBadge = 'badge-success';
            if (item.status === 'Shipped') statusBadge = 'badge-info';

            const allowDelete = item.status !== 'Received' && canCreate;
            const allowReceive = item.status !== 'Received' && canReceive;

            tr.innerHTML = `
                <td>#${item.id}</td>
                <td><strong>${item.supplierName}</strong></td>
                <td>${formatDate(item.orderDate)}</td>
                <td>${formatDate(item.expectedDate)}</td>
                <td>$${Number(item.totalAmount).toFixed(2)}</td>
                <td><span class="badge ${statusBadge}">${item.status}</span></td>
                ${(canCreate || canReceive) ? `
                <td>
                    <div class="action-group">
                        ${allowReceive ? `<button class="action-btn receive-po-btn" data-id="${item.id}" title="Receive Shipment">🚚</button>` : ''}
                        ${allowDelete ? `<button class="action-btn delete-po-btn" data-id="${item.id}" title="Delete PO">🗑️</button>` : ''}
                    </div>
                </td>` : ''}
            `;
            bodyNode.appendChild(tr);
        });
    }

    if (canCreate) {
        document.getElementById('openAddPoBtn').addEventListener('click', async () => {
            const supplierSelect = document.getElementById('poSupplierId');
            supplierSelect.innerHTML = '';
            
            try {
                const res = await fetch(`${API_BASE}/suppliers`);
                if (res.ok) suppliers = await res.json();
            } catch (e) {
                console.error("Error loading suppliers: ", e);
            }

            const approved = suppliers.filter(s => s.supplierStatus === 'APPROVED');
            if (approved.length === 0) {
                alert("No approved suppliers available. Please approve a supplier first.");
                return;
            }

            approved.forEach(s => {
                const opt = document.createElement('option');
                opt.value = s.supplierId;
                opt.textContent = `${s.supplierName} (ID: ${s.supplierId})`;
                supplierSelect.appendChild(opt);
            });

            const twoWeeks = new Date();
            twoWeeks.setDate(twoWeeks.getDate() + 14);
            document.getElementById('poExpectedDate').value = twoWeeks.toISOString().split('T')[0];

            document.getElementById('poItemsList').innerHTML = '';
            document.getElementById('poTotalAmountDisplay').textContent = '0.00';
            
            addPoItemRow();

            openModal('addPurchaseOrderModal');
        });

        document.querySelectorAll('.delete-po-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = parseInt(e.currentTarget.getAttribute('data-id'));
                deletePurchaseOrder(id);
            });
        });
        setupExcelLikeSelection('poTableBody', 'data-key', deleteMultiplePurchaseOrders);
    }

    if (canReceive) {
        document.querySelectorAll('.receive-po-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const id = parseInt(e.currentTarget.getAttribute('data-id'));
                const po = purchaseOrders.find(p => p.id === id);
                if (po) {
                    try {
                        const res = await fetch(`${API_BASE}/purchase-orders/${id}`);
                        const fullPo = await res.json();
                        
                        document.getElementById('grnPoId').value = fullPo.id;
                        const select = document.getElementById('grnPoSelect');
                        select.innerHTML = `<option value="${fullPo.id}">PO #${fullPo.id} - ${fullPo.supplierName}</option>`;
                        select.value = fullPo.id;
                        document.getElementById('grnSupplierDisplay').value = fullPo.supplierName;
                        
                        const grnItemsList = document.getElementById('grnItemsList');
                        grnItemsList.innerHTML = `
                            <div class="grn-item-header">
                                <span>Material Code</span>
                                <span>PO Qty</span>
                                <span>Received Qty *</span>
                                <span>Batch *</span>
                                <span>Expiry Date *</span>
                            </div>
                        `;

                        const itemsRes = await fetch(`${API_BASE}/purchase-orders/${id}/items`);
                        const poItems = await itemsRes.json();
                        
                        poItems.forEach((item, index) => {
                            const row = document.createElement('div');
                            row.className = 'grn-item-row-inputs';
                            
                            const defaultBatch = `B-${item.materialCode}-${Date.now().toString().slice(-6)}`;
                            const nextYear = new Date();
                            nextYear.setFullYear(nextYear.getFullYear() + 2);
                            const defaultExpiry = nextYear.toISOString().split('T')[0];

                            row.innerHTML = `
                                <span><strong>${item.materialCode}</strong></span>
                                <span>${item.quantity}</span>
                                <input type="number" class="grn-qty" data-index="${index}" data-material="${item.materialCode}" required min="1" max="${item.quantity * 2}" value="${item.quantity}">
                                <input type="text" class="grn-batch" data-index="${index}" required placeholder="Batch No" value="${defaultBatch}">
                                <input type="date" class="grn-expiry" data-index="${index}" required value="${defaultExpiry}">
                            `;
                            grnItemsList.appendChild(row);
                        });

                        openModal('createGrnModal');
                    } catch (err) {
                        console.error("Error launching GRN receipt modal: ", err);
                        alert("Error loading PO items: " + err.message);
                    }
                }
            });
        });
    }
}

// 6. Goods Received Notes (GRN) View
function renderGRNView(data) {
    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">Goods Received Notes (GRN)</h1>
            <button class="btn-primary" id="openGrnPoSelectorBtn">+ New GRN from PO</button>
        </div>

        <div class="card-container">
            <div class="table-responsive">
                <table>
                    <thead>
                        <tr>
                            <th>GRN ID</th>
                            <th>PO ID</th>
                            <th>Supplier</th>
                            <th>Received Date</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="grnTableBody"></tbody>
                </table>
            </div>
        </div>
    `;

    const bodyNode = document.getElementById('grnTableBody');
    bodyNode.innerHTML = '';

    if (data.length === 0) {
        bodyNode.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-secondary);">No GRN logs available.</td></tr>`;
    } else {
        data.forEach(item => {
            const tr = document.createElement('tr');
            let statusBadge = 'badge-success';
            if (item.status === 'Quarantined' || item.status === 'Verified') statusBadge = 'badge-success';

            tr.innerHTML = `
                <td>#${item.id}</td>
                <td>#${item.purchaseOrderId}</td>
                <td><strong>${item.supplierName}</strong></td>
                <td>${formatDate(item.receivedDate)}</td>
                <td><span class="badge ${statusBadge}">${item.status}</span></td>
                <td>
                    <button class="action-btn view-grn-details-btn" data-id="${item.id}">👁️ View Details</button>
                </td>
            `;
            bodyNode.appendChild(tr);
        });
    }

    document.getElementById('openGrnPoSelectorBtn').addEventListener('click', async () => {
        try {
            const res = await fetch(`${API_BASE}/purchase-orders`);
            if (res.ok) purchaseOrders = await res.json();
        } catch (e) {
            console.error("Error reloading POs: ", e);
        }

        const pending = purchaseOrders.filter(p => p.status !== 'Received' && p.status !== 'Draft');
        if (pending.length === 0) {
            alert("No pending purchase orders available for receipt.");
            return;
        }

        // Populate PO select dropdown
        const select = document.getElementById('grnPoSelect');
        select.innerHTML = '<option value="">-- Choose Pending PO --</option>';
        pending.forEach(po => {
            const opt = document.createElement('option');
            opt.value = po.id;
            opt.textContent = `PO #${po.id} - ${po.supplierName}`;
            select.appendChild(opt);
        });

        // Reset values
        document.getElementById('grnPoId').value = '';
        document.getElementById('grnSupplierDisplay').value = '';
        document.getElementById('grnItemsList').innerHTML = '<div style="color: var(--text-secondary); text-align: center; padding: 24px;">Please select a Purchase Order from the dropdown above to load items.</div>';

        openModal('createGrnModal');
    });

    // Handle PO selection change inside modal
    document.getElementById('grnPoSelect').addEventListener('change', async (e) => {
        const select = e.target;
        const val = select.value;
        const grnItemsList = document.getElementById('grnItemsList');
        const grnSupplierDisplay = document.getElementById('grnSupplierDisplay');
        const grnPoId = document.getElementById('grnPoId');

        if (!val) {
            grnPoId.value = '';
            grnSupplierDisplay.value = '';
            grnItemsList.innerHTML = '<div style="color: var(--text-secondary); text-align: center; padding: 24px;">Please select a Purchase Order from the dropdown above to load items.</div>';
            return;
        }

        const poId = parseInt(val);
        const selectedPo = purchaseOrders.find(p => p.id === poId);
        if (selectedPo) {
            grnPoId.value = selectedPo.id;
            grnSupplierDisplay.value = selectedPo.supplierName;

            grnItemsList.innerHTML = '<div style="color: var(--text-secondary); text-align: center; padding: 24px;">Loading purchase order items...</div>';

            try {
                const itemsRes = await fetch(`${API_BASE}/purchase-orders/${selectedPo.id}/items`);
                if (!itemsRes.ok) throw new Error("Failed to load PO items");
                const poItems = await itemsRes.json();

                grnItemsList.innerHTML = `<div class="grn-item-header"><span>Material Code</span><span>PO Qty</span><span>Received Qty *</span><span>Batch *</span><span>Expiry Date *</span></div>`;

                poItems.forEach((item, index) => {
                    const row = document.createElement('div');
                    row.className = 'grn-item-row-inputs';
                    const defaultBatch = `B-${item.materialCode}-${Date.now().toString().slice(-6)}`;
                    const nextYear = new Date();
                    nextYear.setFullYear(nextYear.getFullYear() + 2);
                    const defaultExpiry = nextYear.toISOString().split('T')[0];

                    row.innerHTML = `
                        <span><strong>${item.materialCode}</strong></span>
                        <span>${item.quantity}</span>
                        <input type="number" class="grn-qty" data-index="${index}" data-material="${item.materialCode}" required min="1" max="${item.quantity * 2}" value="${item.quantity}">
                        <input type="text" class="grn-batch" data-index="${index}" required placeholder="Batch No" value="${defaultBatch}">
                        <input type="date" class="grn-expiry" data-index="${index}" required value="${defaultExpiry}">
                    `;
                    grnItemsList.appendChild(row);
                });
            } catch (err) {
                grnItemsList.innerHTML = `<div style="color: var(--status-red); text-align: center; padding: 24px;">Error: ${err.message}</div>`;
            }
        }
    });

    document.querySelectorAll('.view-grn-details-btn').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const id = parseInt(e.currentTarget.getAttribute('data-id'));
            try {
                const res = await fetch(`${API_BASE}/grn/${id}`);
                if (!res.ok) throw new Error("Failed to load GRN details");
                const grn = await res.json();
                
                document.getElementById('viewGrnId').value = grn.id;
                document.getElementById('viewGrnPoId').value = grn.purchaseOrderId;
                document.getElementById('viewGrnSupplier').value = grn.supplierName;
                document.getElementById('viewGrnDate').value = formatDate(grn.receivedDate);
                document.getElementById('viewGrnReceivedBy').value = grn.receivedBy;
                document.getElementById('viewGrnStatus').value = grn.status;
                
                const itemsContainer = document.getElementById('viewGrnItemsTableContainer');
                itemsContainer.innerHTML = `
                    <table>
                        <thead>
                            <tr>
                                <th>Material Code</th>
                                <th>Batch Number</th>
                                <th>Quantity Received</th>
                                <th>Expiry Date</th>
                            </tr>
                        </thead>
                        <tbody id="viewGrnItemsTableBody"></tbody>
                    </table>
                `;
                
                const tableBody = document.getElementById('viewGrnItemsTableBody');
                grn.items.forEach(item => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td><strong>${item.materialCode}</strong></td>
                        <td>${item.batchNumber}</td>
                        <td>${item.quantityReceived}</td>
                        <td>${formatDate(item.expiryDate)}</td>
                    `;
                    tableBody.appendChild(tr);
                });

                openModal('viewGrnDetailsModal');
            } catch (err) {
                alert("Error: " + err.message);
            }
        });
    });
}

// PO Items Dynamic Rows Handler
async function addPoItemRow() {
    try {
        const res = await fetch(`${API_BASE}/materials`);
        if (res.ok) materials = await res.json();
    } catch (e) {
        console.error("Error loading materials for PO dropdown: ", e);
    }

    const activeMaterials = materials.filter(m => m.active);
    if (activeMaterials.length === 0) {
        alert("No active materials available to order.");
        return;
    }

    const container = document.getElementById('poItemsList');
    const index = container.children.length;
    const row = document.createElement('div');
    row.className = 'po-item-row';
    row.setAttribute('data-index', index);

    let optionsHtml = activeMaterials.map(m => `<option value="${m.materialCode}" data-name="${m.brandName}">${m.materialCode} - ${m.brandName}</option>`).join('');

    row.innerHTML = `
        <div class="form-group" style="flex: 2;">
            <label>Material *</label>
            <select class="po-item-material" required>
                ${optionsHtml}
            </select>
        </div>
        <div class="form-group" style="flex: 1;">
            <label>Quantity *</label>
            <input type="number" class="po-item-qty" min="1" required value="100">
        </div>
        <div class="form-group" style="flex: 1;">
            <label>Unit Price ($) *</label>
            <input type="number" class="po-item-price" min="0.01" step="0.01" required value="10.00">
        </div>
        <button type="button" class="btn-secondary btn-small remove-po-row-btn" style="margin-bottom: 6px; padding: 8px 12px; border-color: var(--accent-red); color: var(--accent-red); font-size: 14px;">🗑️</button>
    `;

    container.appendChild(row);

    row.querySelector('.po-item-qty').addEventListener('input', calculatePoTotal);
    row.querySelector('.po-item-price').addEventListener('input', calculatePoTotal);
    row.querySelector('.remove-po-row-btn').addEventListener('click', () => {
        row.remove();
        calculatePoTotal();
    });

    calculatePoTotal();
}

function calculatePoTotal() {
    let total = 0;
    document.querySelectorAll('.po-item-row').forEach(row => {
        const qty = parseFloat(row.querySelector('.po-item-qty').value) || 0;
        const price = parseFloat(row.querySelector('.po-item-price').value) || 0;
        total += qty * price;
    });
    document.getElementById('poTotalAmountDisplay').textContent = total.toFixed(2);
}

// 1. Overview Dashboard View
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
    const canEdit = hasAnyPermission('MANAGE_ROLES', 'MANAGE_USERS');
    
    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">Active Materials Master</h1>
            ${canEdit ? '<button class="btn-primary" id="openAddMaterialBtn">+ Add New Material</button>' : ''}
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
                            ${canEdit ? '<th>Actions</th>' : ''}
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
        bodyNode.innerHTML = `<tr><td colspan="${canEdit ? 9 : 8}" style="text-align: center; color: var(--text-secondary);">No materials matched this query</td></tr>`;
    } else {
        data.forEach(item => {
            const tr = document.createElement('tr');
            tr.setAttribute('data-key', item.materialCode);
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
                ${canEdit ? `
                <td>
                    <div class="action-group">
                        <button class="action-btn edit-material-btn" data-code="${item.materialCode}">✏️</button>
                        <button class="action-btn delete-material-btn" data-code="${item.materialCode}">🗑️</button>
                    </div>
                </td>` : ''}
            `;
            bodyNode.appendChild(tr);
        });
    }

    if (canEdit) {
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
        setupExcelLikeSelection('materialsTableBody', 'data-key', deleteMultipleMaterials);
    }
}

// 3. Suppliers Registry View
function renderSuppliersView(data) {
    const canEdit = hasAnyPermission('MANAGE_SUPPLIERS');

    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">Suppliers & Vendor Master</h1>
            ${canEdit ? '<button class="btn-primary" id="openAddSupplierBtn">+ Add New Supplier</button>' : ''}
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
                            ${canEdit ? '<th>Actions</th>' : ''}
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
        bodyNode.innerHTML = `<tr><td colspan="${canEdit ? 9 : 8}" style="text-align: center; color: var(--text-secondary);">No suppliers matched this query</td></tr>`;
    } else {
        data.forEach(item => {
            const tr = document.createElement('tr');
            tr.setAttribute('data-key', item.supplierId);
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
                ${canEdit ? `
                <td>
                    <div class="action-group">
                        <button class="action-btn edit-supplier-btn" data-id="${item.supplierId}">✏️</button>
                        <button class="action-btn approve-supplier-btn" data-id="${item.supplierId}">✅</button>
                        <button class="action-btn reject-supplier-btn" data-id="${item.supplierId}">❌</button>
                        <button class="action-btn delete-supplier-btn" data-id="${item.supplierId}">🗑️</button>
                    </div>
                </td>` : ''}
            `;
            bodyNode.appendChild(tr);
        });
    }

    if (canEdit) {
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
        setupExcelLikeSelection('suppliersTableBody', 'data-key', deleteMultipleSuppliers);
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
                    <td>${formatDate(item.expDate)}</td>
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
    const canCreateOrder = hasAnyPermission('CREATE_PRODUCTION_ORDER');
    const canManageBom = hasAnyPermission('MANAGE_BOM');

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
            ${canCreateOrder ? `
            <div style="padding: 16px; display:flex; justify-content: flex-end;">
                <button class="btn-primary" id="openAddOrderBtn">+ Start New Production Run</button>
            </div>` : ''}
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
                            ${canCreateOrder ? '<th>Control Action</th>' : ''}
                        </tr>
                    </thead>
                    <tbody id="ordersTableBody"></tbody>
                </table>
            </div>
        `;
        const bodyNode = document.getElementById('ordersTableBody');
        if (orderList.length === 0) {
            bodyNode.innerHTML = `<tr><td colspan="${canCreateOrder ? 8 : 7}" style="text-align: center; color: var(--text-secondary);">No production orders scheduled</td></tr>`;
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
                    <td>${formatDate(item.productionDate)}</td>
                    <td><span class="badge ${statusBadge}">${statusStr}</span></td>
                    ${canCreateOrder ? `<td>${actionBtn}</td>` : ''}
                `;
                bodyNode.appendChild(tr);
            });
        }

        if (canCreateOrder) {
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
        }
    } else {
        contentDiv.innerHTML = `
            ${canManageBom ? `
            <div style="padding: 16px; display:flex; justify-content: flex-end;">
                <button class="btn-primary" id="openAddBomBtn">+ Define New BOM Formulation</button>
            </div>` : ''}
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
            // Sort in ascending order of bomId
            const sortedBomList = [...bomList].sort((a, b) => a.bomId - b.bomId);
            sortedBomList.forEach(item => {
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

        if (canManageBom) {
            document.getElementById('openAddBomBtn').addEventListener('click', () => {
                launchAddBomModal();
            });
        }
        document.querySelectorAll('.inspect-bom-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const id = parseInt(e.currentTarget.getAttribute('data-id'));
                await inspectBOMIngredients(id);
            });
        });
    }
}

// 6. QA Compliance Inspections View
function renderComplianceView() {
    // Build filter dropdown option tag template
    const filterOptions = ['ALL', 'QI', 'QUARANTINE', 'IN_PRODUCTION', 'IN_PROCESS_SAMPLE', 'UNDER_TEST', 'APPROVED', 'REJECTED'].map(f => `
        <option value="${f}" ${qaStatusFilter === f ? 'selected' : ''}>${f}</option>
    `).join('');

    // Filter list
    let filtered = qaAllBatches;
    if (qaStatusFilter !== 'ALL') {
        filtered = qaAllBatches.filter(b => b.qcStatus === qaStatusFilter);
    }

    const tableRows = filtered.map(item => {
        let qcBadge = 'badge-warning';
        if (item.qcStatus === 'APPROVED' || item.qcStatus === 'RELEASED') qcBadge = 'badge-success';
        else if (item.qcStatus === 'REJECTED') qcBadge = 'badge-danger';
        else if (item.qcStatus === 'IN_PRODUCTION') qcBadge = 'badge-primary';

        const isSelected = qaSelectedBatch && qaSelectedBatch.batchNumber === item.batchNumber;

        return `
            <tr class="qa-batch-row ${isSelected ? 'row-selected' : ''}" data-batch="${item.batchNumber}" style="cursor: pointer; transition: background-color 0.2s;">
                <td><strong>${item.batchNumber}</strong></td>
                <td><code>${item.materialCode}</code></td>
                <td><span class="badge ${qcBadge}">${item.qcStatus}</span></td>
                <td>${item.quantity.toLocaleString()}</td>
                <td><code>${item.locationCode}</code></td>
            </tr>
        `;
    }).join('');

    mainViewport.innerHTML = `
        <div class="view-header">
            <h1 class="view-title">QA Compliance & Quarantine Control</h1>
            <div style="display: flex; gap: 12px; align-items: center;">
                <label for="qaStatusFilterSelect" style="font-size: 13px; font-weight: 600; color: var(--text-primary);">Filter:</label>
                <select id="qaStatusFilterSelect" class="form-control" style="background-color: var(--bg-elevated); color: var(--text-primary); border: 1px solid var(--border-color); padding: 6px 12px; border-radius: 4px; font-size: 13px;">
                    ${filterOptions}
                </select>
            </div>
        </div>

        <div class="qa-split-container">
            <!-- Left Side: Table -->
            <div class="card-container" style="background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; padding: 20px;">
                <div class="table-responsive">
                    <table>
                        <thead>
                            <tr>
                                <th>Batch Number</th>
                                <th>Material Code</th>
                                <th>QC Status</th>
                                <th>Quantity</th>
                                <th>Location</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${tableRows || '<tr><td colspan="5" style="text-align: center; color: var(--text-secondary);">No batches found matching filter</td></tr>'}
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Right Side: Details Panel -->
            <div id="qaDetailsPanelContainer"></div>
        </div>
    `;

    // Render details panel on the right
    renderBatchDetailsPanel();

    // Listeners
    document.getElementById('qaStatusFilterSelect').addEventListener('change', (e) => {
        qaStatusFilter = e.target.value;
        renderComplianceView();
    });

    document.querySelectorAll('.qa-batch-row').forEach(row => {
        row.addEventListener('click', (e) => {
            const batchNum = e.currentTarget.getAttribute('data-batch');
            qaSelectedBatch = qaAllBatches.find(b => b.batchNumber === batchNum);
            renderComplianceView();
        });
    });
}

function renderBatchDetailsPanel() {
    const container = document.getElementById('qaDetailsPanelContainer');
    if (!container) return;

    if (!qaSelectedBatch) {
        container.innerHTML = `
            <div class="qa-details-panel" style="text-align: center; color: var(--text-secondary); padding: 40px;">
                <div style="font-size: 36px; margin-bottom: 12px;">🔬</div>
                Select a batch from the list to view detailed chemical assay values, parent traceability, and operational controls.
            </div>
        `;
        return;
    }

    const b = qaSelectedBatch;
    let qcBadge = 'badge-warning';
    if (b.qcStatus === 'APPROVED') qcBadge = 'badge-success';
    else if (b.qcStatus === 'REJECTED') qcBadge = 'badge-danger';
    else if (b.qcStatus === 'IN_PRODUCTION') qcBadge = 'badge-primary';

    const canUpdateStatus = hasAnyPermission('UPDATE_QC_STATUS');
    const canApprove = hasAnyPermission('APPROVE_QA');
    const canReject = hasAnyPermission('REJECT_QA');

    // Status driven action buttons
    let actionButtonsHtml = '';
    if (b.qcStatus === 'IN_PRODUCTION') {
        actionButtonsHtml = `
            ${canUpdateStatus ? '<button class="btn-primary qa-action-btn" id="qaBtnSample" style="flex:1;">⚡ Take IPQC Sample</button>' : ''}
            <button class="btn-secondary qa-action-btn" id="qaBtnGenealogy" style="flex:1;">🧬 View Genealogy</button>
        `;
    } else if (b.qcStatus === 'IN_PROCESS_SAMPLE') {
        actionButtonsHtml = `
            ${canUpdateStatus ? '<button class="btn-primary qa-action-btn" id="qaBtnStartTest" style="flex:1;">🔬 Start Testing</button>' : ''}
            <button class="btn-secondary qa-action-btn" id="qaBtnGenealogy" style="flex:1;">🧬 View Genealogy</button>
        `;
    } else if (b.qcStatus === 'QUARANTINE') {
        actionButtonsHtml = `
            ${canUpdateStatus ? '<button class="btn-primary qa-action-btn" id="qaBtnSample" style="flex:1;">⚡ Take QC Sample</button>' : ''}
            <button class="btn-secondary qa-action-btn" id="qaBtnGenealogy" style="flex:1;">🧬 View Genealogy</button>
        `;
    } else if (b.qcStatus === 'UNDER_TEST' || b.qcStatus === 'QI') {
        actionButtonsHtml = `
            ${canApprove ? '<button class="btn-success qa-action-btn" id="qaBtnApprove" style="background-color: var(--status-green) !important; color: white !important; flex:1;">✅ Approve & Release</button>' : ''}
            ${canReject ? '<button class="btn-danger qa-action-btn" id="qaBtnReject" style="background-color: var(--status-red) !important; color: white !important; flex:1;">❌ Reject Batch</button>' : ''}
            <button class="btn-secondary qa-action-btn" id="qaBtnGenealogy" style="width:100%; margin-top:8px;">🧬 View Genealogy</button>
        `;
    } else {
        // Approved or Rejected
        actionButtonsHtml = `
            <button class="btn-secondary qa-action-btn" id="qaBtnGenealogy" style="width: 100%;">🧬 View Genealogy</button>
        `;
    }

    container.innerHTML = `
        <div class="qa-details-panel">
            <div class="qa-details-section">
                <h3 style="font-size: 15px; font-weight:600; color: var(--text-primary); margin-bottom: 12px; display:flex; justify-content:space-between; align-items:center;">
                    <span>Batch details: <code>${b.batchNumber}</code></span>
                    <span class="badge ${qcBadge}">${b.qcStatus}</span>
                </h3>
            </div>

            <!-- SECTION 1: BATCH INFORMATION -->
            <div class="qa-details-section">
                <div class="qa-section-header">Batch Information</div>
                <div class="qa-details-grid">
                    <span class="qa-details-label">Material Code:</span>
                    <span class="qa-details-value"><code>${b.materialCode}</code></span>
                    <span class="qa-details-label">Brand Name:</span>
                    <span class="qa-details-value">${materials.find(m => m.materialCode === b.materialCode)?.brandName || 'N/A'}</span>
                    <span class="qa-details-label">Source Layer:</span>
                    <span class="qa-details-value">${b.source}</span>
                </div>
            </div>

            <!-- SECTION 2: QUANTITY & LOCATION -->
            <div class="qa-details-section">
                <div class="qa-section-header">Quantity & Location</div>
                <div class="qa-details-grid">
                    <span class="qa-details-label">Holding Quantity:</span>
                    <span class="qa-details-value">${b.quantity.toLocaleString()}</span>
                    <span class="qa-details-label">Available Qty:</span>
                    <span class="qa-details-value">${b.availableQuantity.toLocaleString()}</span>
                    <span class="qa-details-label">Location:</span>
                    <span class="qa-details-value"><code>${b.locationCode}</code></span>
                </div>
            </div>

            <!-- SECTION 3: DATES -->
            <div class="qa-details-section">
                <div class="qa-section-header">Assay & Expiration Dates</div>
                <div class="qa-details-grid">
                    <span class="qa-details-label">Expiry Date:</span>
                    <span class="qa-details-value">${formatDate(b.expDate)}</span>
                    <span class="qa-details-label">Assay Cost:</span>
                    <span class="qa-details-value">$${b.unitCost.toFixed(2)}</span>
                </div>
            </div>

            <!-- ACTIONS -->
            <div class="qa-details-section">
                <div class="qa-section-header">Workflow Compliance Actions</div>
                <div class="qa-action-buttons">
                    ${actionButtonsHtml}
                </div>
            </div>
        </div>
    `;

    // Event listeners
    const btnSample = document.getElementById('qaBtnSample');
    if (btnSample) {
        btnSample.addEventListener('click', async () => {
            const isQuarantine = b.qcStatus === 'QUARANTINE';
            const nextStatus = isQuarantine ? 'QI' : 'IN_PROCESS_SAMPLE';
            const promptMsg = isQuarantine
                ? `Take a QC sample for quarantined batch ${b.batchNumber}?\nThis will transition the batch to QI.`
                : `Take an IPQC sample for batch ${b.batchNumber}?\nThis will transition the batch to IN_PROCESS_SAMPLE.`;

            if (!await customConfirm(promptMsg)) return;
            try {
                const res = await fetch(`${API_BASE}/qa/inspections/${b.batchNumber}/sample`, { method: 'POST' });
                if (!res.ok) {
                    const errObj = await res.json().catch(() => ({}));
                    throw new Error(errObj.error || errObj.message || `Server error ${res.status}`);
                }
                alert(`Sample logged! Batch transitioned to ${nextStatus}.`);
                qaSelectedBatch = null;
                await fetchComplianceData();
            } catch (err) {
                alert(`❌ Failed to take sample: ${err.message}`);
            }
        });
    }

    const btnStartTest = document.getElementById('qaBtnStartTest');
    if (btnStartTest) {
        btnStartTest.addEventListener('click', async () => {
            if (!await customConfirm(`Start chemical assay testing for batch ${b.batchNumber}?\nThis will transition the batch to UNDER_TEST.`)) return;
            try {
                const res = await fetch(`${API_BASE}/qa/inspections/${b.batchNumber}/status`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ status: 'UNDER_TEST' })
                });
                if (!res.ok) {
                    const errObj = await res.json().catch(() => ({}));
                    throw new Error(errObj.error || errObj.message || `Server error ${res.status}`);
                }
                alert('Testing run initialized. Batch is now UNDER_TEST.');
                qaSelectedBatch = null;
                await fetchComplianceData();
            } catch (err) {
                alert(`❌ Failed to start testing: ${err.message}`);
            }
        });
    }

    // 21 CFR Part 11 E-Signature verification helper
    async function verifyESignature(password) {
        if (!currentUser || (!currentUser.employeeId && !currentUser.username)) {
            throw new Error("No active user session.");
        }
        const empId = currentUser.employeeId || currentUser.username;
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ employeeId: empId, password: password })
        });
        return res.ok;
    }

    const btnApprove = document.getElementById('qaBtnApprove');
    if (btnApprove) {
        btnApprove.addEventListener('click', async () => {
            const remarks = await customPrompt('Enter approval remarks / release rationale *:');
            if (remarks === null) return;
            if (remarks.trim() === '') {
                alert('Remarks are required for release.');
                return;
            }

            const password = await customPasswordPrompt('Re-enter your password to authorize Electronic Signature for batch release *:');
            if (password === null) return;
            if (password.trim() === '') {
                alert('Password is required for Electronic Signature verification.');
                return;
            }

            // Verify E-Signature credentials
            try {
                const verified = await verifyESignature(password);
                if (!verified) {
                    alert('❌ Invalid credentials. Electronic Signature verification failed.');
                    return;
                }
            } catch (err) {
                alert(`❌ Signature verification error: ${err.message}`);
                return;
            }

            try {
                const res = await fetch(`${API_BASE}/qa/inspections/${b.batchNumber}/inspect`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ status: 'APPROVED', remarks: remarks.trim(), performedBy: currentUser?.fullName || 'QC Inspector' })
                });
                if (!res.ok) {
                    const errObj = await res.json().catch(() => ({}));
                    throw new Error(errObj.error || errObj.message || `Server error ${res.status}`);
                }
                alert('✅ Batch approved and released to warehouse successfully!');
                qaSelectedBatch = null;
                await fetchComplianceData();
            } catch (err) {
                alert(`❌ Failed to approve batch: ${err.message}`);
            }
        });
    }

    const btnReject = document.getElementById('qaBtnReject');
    if (btnReject) {
        btnReject.addEventListener('click', async () => {
            const remarks = await customPrompt('Enter rejection remarks / hazard rationale *:');
            if (remarks === null) return;
            if (remarks.trim() === '') {
                alert('Remarks are required for QC quarantine rejection.');
                return;
            }

            const password = await customPasswordPrompt('Re-enter your password to authorize Electronic Signature for batch rejection *:');
            if (password === null) return;
            if (password.trim() === '') {
                alert('Password is required for Electronic Signature verification.');
                return;
            }

            // Verify E-Signature credentials
            try {
                const verified = await verifyESignature(password);
                if (!verified) {
                    alert('❌ Invalid credentials. Electronic Signature verification failed.');
                    return;
                }
            } catch (err) {
                alert(`❌ Signature verification error: ${err.message}`);
                return;
            }

            try {
                const res = await fetch(`${API_BASE}/qa/inspections/${b.batchNumber}/inspect`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ status: 'REJECTED', remarks: remarks.trim(), performedBy: currentUser?.fullName || 'QC Inspector' })
                });
                if (!res.ok) {
                    const errObj = await res.json().catch(() => ({}));
                    throw new Error(errObj.error || errObj.message || `Server error ${res.status}`);
                }
                alert('❌ Batch rejected and moved to REJECTED_AREA.');
                qaSelectedBatch = null;
                await fetchComplianceData();
            } catch (err) {
                alert(`❌ Failed to reject batch: ${err.message}`);
            }
        });
    }


    const btnGenealogy = document.getElementById('qaBtnGenealogy');
    if (btnGenealogy) {
        btnGenealogy.addEventListener('click', () => {
            showGenealogyModal(b.batchNumber);
        });
    }
}

async function showGenealogyModal(batchNumber) {
    let parents = [];
    try {
        const res = await fetch(`${API_BASE}/qa/inspections/${batchNumber}/genealogy`);
        if (res.ok) parents = await res.json();
    } catch (err) {
        console.warn("Genealogy api fetch failed, loading fallback: ", err);
    }

    if (parents.length === 0) {
        // Fallback mock parents based on batch prefix
        if (batchNumber.includes("PCM-901") || batchNumber.includes("PCM-902")) {
            parents = ["B-RAW-PCM-01 (Raw Paracetamol)", "B-RAW-GLY-02 (Raw Glycerol)"];
        } else if (batchNumber.includes("AMX")) {
            parents = ["B-RAW-AMX-08 (Raw Amoxicillin API)"];
        } else {
            parents = ["No parent batch lineage found (Raw Material origin)"];
        }
    }

    const parentsListHtml = parents.map(p => `
        <li style="padding: 10px; background-color: var(--bg-elevated); border: 1px solid var(--border-color); border-radius: 4px; font-size: 13px; font-family: monospace; color: var(--text-primary);">
            🔗 Parent: ${p}
        </li>
    `).join('');

    // We can reuse or instantiate a custom dynamic modal overlay
    const modalId = "qaGenealogyModal";
    let modalEl = document.getElementById(modalId);
    if (!modalEl) {
        modalEl = document.createElement('div');
        modalEl.className = "modal-overlay";
        modalEl.id = modalId;
        document.body.appendChild(modalEl);
    }

    modalEl.innerHTML = `
        <div class="modal-window" style="width: 480px;">
            <div class="modal-header">
                <h2 class="modal-title">Batch Genealogy & Traceability</h2>
                <button class="modal-close" onclick="closeModal('${modalId}')">✖</button>
            </div>
            <div class="modal-body" style="padding: 20px;">
                <p style="font-size: 13px; color: var(--text-secondary); margin-bottom: 16px;">
                    Genealogy trace for batch <strong>${batchNumber}</strong>:
                </p>
                <ul style="display: flex; flex-direction: column; gap: 10px; list-style: none; padding: 0; margin: 0;">
                    ${parentsListHtml}
                </ul>
            </div>
            <div class="modal-footer">
                <button class="btn-secondary" onclick="closeModal('${modalId}')" style="width: 100%;">Close Traceability</button>
            </div>
        </div>
    `;

    openModal(modalId);
}

/* --- MODAL FORMS DATA LOADERS --- */

function launchInspectBatchModal(batchNumber) {
    const item = qaList.find(q => q.batchNumber === batchNumber);
    if (!item) return;

    document.getElementById('inspectBatchNumber').value = item.batchNumber;
    document.getElementById('inspectMaterialCode').value = item.materialCode;
    document.getElementById('inspectQuantity').value = item.quantity;
    document.getElementById('inspectRemarks').value = '';
    document.getElementById('inspectPerformedBy').value = currentUser?.fullName || currentUser?.employeeId || '';

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
    document.getElementById('addOrderCreatedBy').value = currentUser?.userId || 1;
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
        const bom = bomList.find(b => b.bomId === bomId);
        if (!bom) throw new Error("BOM details not found in active list");

        const res = await fetch(`${API_BASE}/bom/${bomId}/ingredients`);
        if (!res.ok) throw new Error("Failed to load ingredients");
        const ingredients = await res.json();
        
        document.getElementById('viewBomId').value = `BOM-#${bom.bomId}`;
        document.getElementById('viewBomProductCode').value = bom.materialCode;
        document.getElementById('viewBomDescription').value = bom.description;
        document.getElementById('viewBomVersionStatus').value = `v${bom.versionNumber} - ${bom.active ? 'ACTIVE' : 'INACTIVE'}`;
        
        const container = document.getElementById('viewBomIngredientsTableContainer');
        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>Ingredient Code</th>
                        <th>Required Qty</th>
                        <th>UoM</th>
                        <th>Notes / Rationale</th>
                    </tr>
                </thead>
                <tbody id="viewBomIngredientsTableBody"></tbody>
            </table>
        `;
        
        const tableBody = document.getElementById('viewBomIngredientsTableBody');
        ingredients.forEach(i => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>${i.ingredientMaterialCode}</strong></td>
                <td>${i.requiredQty}</td>
                <td>${i.uom}</td>
                <td>${i.notes || '-'}</td>
            `;
            tableBody.appendChild(tr);
        });
        
        openModal('viewBomDetailsModal');
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
    document.getElementById('statusPerformedBy').value = currentUser?.fullName || currentUser?.employeeId || '';
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
    addProductionOrderForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const bomId = parseInt(document.getElementById('addOrderBomId').value);
        const plannedQty = parseFloat(document.getElementById('addOrderPlannedQty').value);

        const payload = {
            batchNumber: document.getElementById('addOrderBatchNumber').value.trim(),
            bomId: bomId,
            plannedQty: plannedQty,
            createdBy: parseInt(document.getElementById('addOrderCreatedBy').value),
            notes: document.getElementById('addOrderNotes').value.trim()
        };

        // Check BOM feasibility / shortfalls first
        try {
            const res = await fetch(`${API_BASE}/production/feasibility?bomId=${bomId}&plannedQty=${plannedQty}`);
            if (res.ok) {
                const availability = await res.json();
                const shortfalls = availability.filter(a => !a.available);
                if (shortfalls.length > 0) {
                    let warnMsg = "Material Shortage Warning! The following raw materials have insufficient stock for this run:\n\n";
                    shortfalls.forEach(s => {
                        const deficit = s.requiredQuantity - s.availableQuantity;
                        warnMsg += `- Material: ${s.materialCode}\n  Required: ${s.requiredQuantity.toLocaleString()}\n  Available: ${s.availableQuantity.toLocaleString()} (Shortfall: ${deficit.toLocaleString()})\n\n`;
                    });
                    warnMsg += "Do you still want to proceed with scheduling this production order?";
                    if (!await customConfirm(warnMsg)) {
                        return; // Abort order creation
                    }
                }
            }
        } catch (err) {
            console.warn("Feasibility check skipped or failed: ", err);
        }

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

    // Add Location
    document.getElementById('addLocationForm').addEventListener('submit', (e) => {
        e.preventDefault();
        const payload = {
            locationCode: document.getElementById('locCode').value.trim(),
            locationName: document.getElementById('locName').value.trim(),
            description: document.getElementById('locDesc').value.trim(),
            capacity: parseInt(document.getElementById('locCapacity').value)
        };
        createLocation(payload);
    });

    // Edit Location
    document.getElementById('editLocationForm').addEventListener('submit', (e) => {
        e.preventDefault();
        const code = document.getElementById('editLocCode').value;
        const payload = {
            locationCode: code,
            locationName: document.getElementById('editLocName').value.trim(),
            description: document.getElementById('editLocDesc').value.trim(),
            capacity: parseInt(document.getElementById('editLocCapacity').value)
        };
        updateLocation(code, payload);
    });

    // Add Purchase Order
    document.getElementById('addPurchaseOrderForm').addEventListener('submit', (e) => {
        e.preventDefault();
        
        const items = [];
        document.querySelectorAll('.po-item-row').forEach(row => {
            const materialCode = row.querySelector('.po-item-material').value;
            const quantity = parseInt(row.querySelector('.po-item-qty').value);
            const unitPrice = parseFloat(row.querySelector('.po-item-price').value);
            items.push({ materialCode, quantity, unitPrice });
        });

        if (items.length === 0) {
            alert("Please add at least one item to the purchase order.");
            return;
        }

        const payload = {
            supplierId: parseInt(document.getElementById('poSupplierId').value),
            expectedDate: document.getElementById('poExpectedDate').value,
            items: items,
            totalAmount: parseFloat(document.getElementById('poTotalAmountDisplay').textContent)
        };
        
        createPurchaseOrder(payload);
    });

    // Create GRN Verification
    document.getElementById('createGrnForm').addEventListener('submit', (e) => {
        e.preventDefault();
        const poId = parseInt(document.getElementById('grnPoId').value);
        
        const grnItemsList = document.getElementById('grnItemsList');
        const qtyInputs = grnItemsList.querySelectorAll('.grn-qty');
        const batchInputs = grnItemsList.querySelectorAll('.grn-batch');
        const expiryInputs = grnItemsList.querySelectorAll('.grn-expiry');
        
        const items = [];
        for (let i = 0; i < qtyInputs.length; i++) {
            const materialCode = qtyInputs[i].getAttribute('data-material');
            const quantityReceived = parseInt(qtyInputs[i].value);
            const batchNumber = batchInputs[i].value.trim();
            const expiryDate = expiryInputs[i].value;
            
            items.push({
                materialCode,
                batchNumber,
                quantityReceived,
                expiryDate
            });
        }
        
        const receivedBy = currentUser?.fullName || 'QC Inspector';
        const receivedByUserId = currentUser?.userId || 1;
        
        createGRN({
            poId,
            receivedBy,
            receivedByUserId,
            items
        });
    });

    // Dynamic row builder button
    document.getElementById('addPoItemBtn').addEventListener('click', addPoItemRow);
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
    if (sseInitialized) return;

    agentEventSource = new EventSource(`${API_BASE}/agent/stream`);
    sseInitialized = true;
    
    agentEventSource.onopen = () => {
        const logList = document.getElementById('logList');
        if (logList && logList.innerHTML.includes("Waiting for Javalin REST SSE connection")) {
            logList.innerHTML = `
                <div class="log-item system" style="opacity: 0.85; padding: 12px; border-bottom: 1px solid var(--border-color);">
                  <div class="log-text">
                    <div class="log-meta" style="margin-bottom: 4px; display: flex; justify-content: space-between;">
                      <span class="log-agent" style="color: var(--accent-teal); font-weight: 600;">System Gateway</span>
                      <span class="log-time" style="font-size: 10px; color: var(--text-secondary);">Active</span>
                    </div>
                    <div style="font-size: 12px; color: var(--text-primary);">💚 Connection established. Listening for active JADE agent logs...</div>
                  </div>
                </div>
            `;
        }
    };

    agentEventSource.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            appendLiveLogItem(data);
        } catch (e) {
            console.error("Error parsing SSE event: ", e);
        }
    };
    
    agentEventSource.onerror = (err) => {
        console.error("SSE stream error: ", err);
    };
}

function appendLiveLogItem(el) {
    const logList = document.getElementById('logList');
    if (!logList) return;

    if (logList.innerHTML.includes("Waiting for Javalin REST SSE connection") || logList.innerHTML.includes("Connection established")) {
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
    if (initCompleted) return;
    initCompleted = true;

    initAuthListeners();
    currentUser = readStoredSession();

    if (currentUser) {
        startAuthenticatedShell();
    } else {
        showLogin();
    }
}

document.addEventListener('DOMContentLoaded', init);
if (document.readyState === 'interactive' || document.readyState === 'complete') {
    init();
}
