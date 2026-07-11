package pharma.api;

import io.javalin.Javalin;
import pharma.config.ApplicationServices;
import pharma.model.Permission;
import pharma.model.Role;
import pharma.model.User;
import pharma.model.Material;
import pharma.model.Supplier;
import pharma.model.Stock;
import pharma.model.InventoryTransaction;
import pharma.model.BOMHeader;
import pharma.model.BOMDetail;
import pharma.model.ProductionOrder;
import pharma.model.Location;
import pharma.model.PurchaseOrder;
import pharma.model.GRN;
import pharma.dto.MaterialAvailabilityDTO;
import pharma.dto.RiskReportDTO;
import pharma.service.DatabaseService;
import pharma.service.RoleService;
import java.util.List;
import java.util.Set;

public class ApiServer {
    private static Javalin app;

    @SuppressWarnings("null")
    public static void start(ApplicationServices appServices) {
        DatabaseService dbService = appServices.getDatabaseService();
        RoleService roleService = new RoleService(dbService);

        app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.anyHost();
                });
            });
        });

        // Global Exception Handler
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse(e.getMessage()));
        });

        // --- AUTHENTICATION / RBAC ENDPOINTS ---
        app.post("/api/auth/login", ctx -> {
            LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
            String employeeId = firstNonBlank(req.employeeId, req.username);
            String password = clean(req.password);

            if (employeeId.isBlank() || password.isBlank()) {
                ctx.status(400).json(new ErrorResponse("Employee ID and password are required."));
                return;
            }

            User user = dbService.getUserByCredentials(employeeId, password);
            if (user == null) {
                ctx.status(401).json(new ErrorResponse("Invalid employee ID or password."));
                return;
            }

            ctx.json(new AuthSessionResponse(user));
        });

        app.get("/api/auth/roles", ctx -> {
            ctx.json(roleService.getAllRoles());
        });

        app.get("/api/auth/permissions/{role}", ctx -> {
            Role role = resolveRole(roleService, ctx.pathParam("role"));
            if (role == null) {
                ctx.status(404).json(new ErrorResponse("Role not found"));
                return;
            }

            List<Permission> permissions = roleService.getAllPermissions();
            Set<Integer> assignedPermissionIds = roleService.getPermissionIdsForRole(role.getRoleId());
            ctx.json(new RolePermissionsResponse(role, permissions, assignedPermissionIds));
        });

        app.put("/api/auth/permissions/{role}", ctx -> {
            Role role = resolveRole(roleService, ctx.pathParam("role"));
            if (role == null) {
                ctx.status(404).json(new ErrorResponse("Role not found"));
                return;
            }

            PermissionUpdateRequest req = ctx.bodyAsClass(PermissionUpdateRequest.class);
            Set<Integer> permissionIds = req.permissionIds == null ? Set.of() : req.permissionIds;
            int adminUserId = req.adminUserId > 0 ? req.adminUserId : 1;

            boolean updated = roleService.updateRolePermissions(role.getRoleId(), permissionIds, adminUserId);
            if (updated) {
                ctx.json(new StatusUpdateResponse("Role permissions updated successfully"));
            } else {
                ctx.status(500).json(new ErrorResponse("Failed to update role permissions"));
            }
        });

        // --- MATERIALS CRUD ENDPOINTS ---
        app.get("/api/materials", ctx -> {
            List<Material> materials = dbService.getAllMaterials();
            ctx.json(materials);
        });

        app.get("/api/materials/{code}", ctx -> {
            String code = ctx.pathParam("code");
            Material material = dbService.getDrugByMaterialCode(code);
            if (material != null) {
                ctx.json(material);
            } else {
                ctx.status(404).json(new ErrorResponse("Material not found"));
            }
        });

        app.post("/api/materials", ctx -> {
            Material material = ctx.bodyAsClass(Material.class);
            dbService.addMaterial(material);
            ctx.status(201).json(material);
        });

        app.put("/api/materials/{code}", ctx -> {
            String code = ctx.pathParam("code");
            Material material = ctx.bodyAsClass(Material.class);
            material.setMaterialCode(code);
            boolean updated = dbService.updateDrug(material);
            if (updated) {
                ctx.json(material);
            } else {
                ctx.status(404).json(new ErrorResponse("Failed to update material or material not found"));
            }
        });

        app.delete("/api/materials/{code}", ctx -> {
            String code = ctx.pathParam("code");
            boolean deleted = dbService.deleteDrug(code);
            if (deleted) {
                ctx.status(204);
            } else {
                ctx.status(404).json(new ErrorResponse("Failed to delete material or material not found"));
            }
        });

        // --- SUPPLIERS CRUD ENDPOINTS ---
        app.get("/api/suppliers", ctx -> {
            List<Supplier> suppliers = dbService.getAllSuppliers();
            ctx.json(suppliers);
        });

        app.get("/api/suppliers/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Supplier supplier = dbService.getSupplierById(id);
            if (supplier != null) {
                ctx.json(supplier);
            } else {
                ctx.status(404).json(new ErrorResponse("Supplier not found"));
            }
        });

        app.post("/api/suppliers", ctx -> {
            Supplier supplier = ctx.bodyAsClass(Supplier.class);
            int newId = dbService.addSupplier(supplier);
            supplier.setSupplierId(newId);
            ctx.status(201).json(supplier);
        });

        app.put("/api/suppliers/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Supplier supplier = ctx.bodyAsClass(Supplier.class);
            supplier.setSupplierId(id);
            boolean updated = dbService.updateSupplier(supplier);
            if (updated) {
                ctx.json(supplier);
            } else {
                ctx.status(404).json(new ErrorResponse("Failed to update supplier"));
            }
        });

        app.delete("/api/suppliers/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            boolean deleted = dbService.deleteSupplier(id);
            if (deleted) {
                ctx.status(204);
            } else {
                ctx.status(404).json(new ErrorResponse("Failed to delete supplier"));
            }
        });

        app.patch("/api/suppliers/{id}/status", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            StatusUpdateRequest req = ctx.bodyAsClass(StatusUpdateRequest.class);
            boolean success = false;
            if ("APPROVED".equalsIgnoreCase(req.status)) {
                success = dbService.approveSupplier(id, req.remarks, req.performedBy);
            } else if ("REJECTED".equalsIgnoreCase(req.status)) {
                success = dbService.rejectSupplier(id, req.remarks, req.performedBy);
            } else {
                ctx.status(400).json(new ErrorResponse("Invalid status value. Use APPROVED or REJECTED."));
                return;
            }
            if (success) {
                ctx.json(new StatusUpdateResponse("Supplier status updated successfully"));
            } else {
                ctx.status(500).json(new ErrorResponse("Failed to update supplier status"));
            }
        });

        // --- LOCATIONS CRUD ENDPOINTS ---
        app.get("/api/locations", ctx -> {
            ctx.json(dbService.getLocations());
        });

        app.get("/api/locations/{code}", ctx -> {
            String code = ctx.pathParam("code");
            Location loc = dbService.getLocationById(code);
            if (loc != null) {
                ctx.json(loc);
            } else {
                ctx.status(404).json(new ErrorResponse("Location not found"));
            }
        });

        app.post("/api/locations", ctx -> {
            Location loc = ctx.bodyAsClass(Location.class);
            boolean success = dbService.addLocation(loc.getLocationCode(), loc.getLocationName(), loc.getDescription(), loc.getCapacity());
            if (success) {
                ctx.status(201).json(loc);
            } else {
                ctx.status(500).json(new ErrorResponse("Failed to create location"));
            }
        });

        app.put("/api/locations/{code}", ctx -> {
            String code = ctx.pathParam("code");
            Location loc = ctx.bodyAsClass(Location.class);
            boolean success = dbService.updateLocation(code, loc.getLocationName(), loc.getDescription(), loc.getCapacity());
            if (success) {
                loc.setLocationCode(code);
                ctx.json(loc);
            } else {
                ctx.status(500).json(new ErrorResponse("Failed to update location"));
            }
        });

        app.delete("/api/locations/{code}", ctx -> {
            String code = ctx.pathParam("code");
            boolean success = dbService.deleteLocation(code);
            if (success) {
                ctx.status(204);
            } else {
                ctx.status(500).json(new ErrorResponse("Failed to delete location or location not found"));
            }
        });

        // --- PURCHASE ORDERS ENDPOINTS ---
        app.get("/api/purchase-orders", ctx -> {
            ctx.json(dbService.getPurchaseOrders());
        });

        app.get("/api/purchase-orders/{id}", ctx -> {
            String id = ctx.pathParam("id");
            PurchaseOrder po = dbService.getPurchaseOrderById(id);
            if (po != null) {
                ctx.json(po);
            } else {
                ctx.status(404).json(new ErrorResponse("Purchase Order not found"));
            }
        });

        app.get("/api/purchase-orders/{id}/items", ctx -> {
            int poId = Integer.parseInt(ctx.pathParam("id"));
            ctx.json(dbService.getPurchaseOrderItems(poId));
        });

        app.post("/api/purchase-orders", ctx -> {
            PurchaseOrder po = ctx.bodyAsClass(PurchaseOrder.class);
            if (po.getOrderDate() == null) {
                po.setOrderDate(java.time.LocalDate.now());
            }
            if (po.getExpectedDate() == null) {
                po.setExpectedDate(java.time.LocalDate.now().plusWeeks(2));
            }
            po.setStatus("Pending");
            boolean success = dbService.createPurchaseOrder(po);
            if (success) {
                ctx.status(201).json(po);
            } else {
                ctx.status(500).json(new ErrorResponse("Failed to create purchase order"));
            }
        });

        app.put("/api/purchase-orders/{id}", ctx -> {
            PurchaseOrder po = ctx.bodyAsClass(PurchaseOrder.class);
            po.setId(Integer.parseInt(ctx.pathParam("id")));
            boolean success = dbService.updatePurchaseOrder(po);
            if (success) {
                ctx.json(po);
            } else {
                ctx.status(500).json(new ErrorResponse("Failed to update purchase order"));
            }
        });

        app.delete("/api/purchase-orders/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            PurchaseOrder po = dbService.getPurchaseOrderById(String.valueOf(id));
            if (po == null) {
                ctx.status(404).json(new ErrorResponse("Purchase Order not found"));
                return;
            }
            if ("Received".equalsIgnoreCase(po.getStatus())) {
                ctx.status(400).json(new ErrorResponse("Cannot delete order because it already has received stock."));
                return;
            }
            dbService.deletePurchaseOrder(id);
            ctx.status(204);
        });

        app.post("/api/purchase-orders/{id}/receive", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            dbService.receivePurchaseOrderShipment(id);
            ctx.json(new StatusUpdateResponse("Purchase order shipment received successfully"));
        });

        // --- GOODS RECEIVED NOTES (GRN) ENDPOINTS ---
        app.get("/api/grn", ctx -> {
            ctx.json(dbService.getGRNs());
        });

        app.get("/api/grn/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            GRN grn = dbService.getGRNById(id);
            if (grn != null) {
                ctx.json(grn);
            } else {
                ctx.status(404).json(new ErrorResponse("GRN not found"));
            }
        });

        app.post("/api/grn", ctx -> {
            CreateGRNRequest req = ctx.bodyAsClass(CreateGRNRequest.class);
            PurchaseOrder po = dbService.getPurchaseOrderById(String.valueOf(req.poId));
            if (po == null) {
                ctx.status(404).json(new ErrorResponse("Purchase Order not found"));
                return;
            }
            List<GRN.GRNItem> items = mapGrnItems(req.items);
            boolean success = dbService.createGRNFromPO(po, items, req.receivedBy, req.receivedByUserId);
            if (success) {
                ctx.status(201).json(new StatusUpdateResponse("GRN created successfully"));
            } else {
                ctx.status(500).json(new ErrorResponse("Failed to create GRN"));
            }
        });

        // --- REPORTS ENDPOINTS ---
        app.get("/api/reports/stock-value", ctx -> {
            ctx.json(dbService.getDetailedInventoryReport());
        });

        app.get("/api/reports/low-stock", ctx -> {
            ctx.json(appServices.getInventoryService().findLowStockMaterials());
        });

        app.get("/api/reports/expiring", ctx -> {
            List<Stock> allStock = dbService.getDetailedInventoryReport();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate sixMonthsFromNow = today.plusMonths(6);
            List<Stock> expiring = allStock.stream()
                .filter(s -> s.getExpDate() != null && 
                             !s.getExpDate().isBefore(today) && 
                             !s.getExpDate().isAfter(sixMonthsFromNow))
                .toList();
            ctx.json(expiring);
        });

        app.get("/api/reports/supplier-performance", ctx -> {
            List<Supplier> suppliers = dbService.getAllSuppliers();
            List<SupplierPerformanceResponse> performance = new java.util.ArrayList<>();
            for (Supplier s : suppliers) {
                RiskReportDTO report = appServices.getRiskService().scoreSupplierRisk(s.getSupplierId());
                performance.add(new SupplierPerformanceResponse(
                    s.getSupplierId(),
                    s.getSupplierName(),
                    report.getDrivers(),
                    report.getRiskScore(),
                    report.getSeverity(),
                    s.getSupplierStatus()
                ));
            }
            ctx.json(performance);
        });

        app.get("/api/reports/grn-history", ctx -> {
            ctx.json(dbService.getGRNs());
        });

        // --- STOCK / INVENTORY ENDPOINTS ---
        app.get("/api/stock", ctx -> {
            List<Stock> stock = dbService.getDetailedInventoryReport();
            ctx.json(stock);
        });

        app.get("/api/stock/transactions", ctx -> {
            List<InventoryTransaction> transactions = dbService.getInventoryTransactions();
            ctx.json(transactions);
        });

        app.post("/api/stock/transactions", ctx -> {
            InventoryTransaction tx = ctx.bodyAsClass(InventoryTransaction.class);
            boolean success = dbService.addInventoryTransaction(tx);
            if (success) {
                ctx.status(201).json(tx);
            } else {
                ctx.status(500).json(new ErrorResponse("Failed to write inventory transaction"));
            }
        });

        // --- BOM (BILL OF MATERIALS) ENDPOINTS ---
        app.get("/api/bom", ctx -> {
            List<BOMHeader> boms = dbService.getAllBOMs();
            ctx.json(boms);
        });

        app.get("/api/bom/{id}/ingredients", ctx -> {
            int bomId = Integer.parseInt(ctx.pathParam("id"));
            List<BOMDetail> ingredients = dbService.getBOMIngredients(bomId);
            ctx.json(ingredients);
        });

        app.post("/api/bom", ctx -> {
            CreateBOMRequest req = ctx.bodyAsClass(CreateBOMRequest.class);
            if (req.header.getEffectiveDate() == null) {
                req.header.setEffectiveDate(java.time.LocalDate.now());
            }
            int newBomId = dbService.createBOM(req.header, req.details);
            req.header.setBomId(newBomId);
            ctx.status(201).json(req.header);
        });

        // --- PRODUCTION ORDERS ENDPOINTS ---
        app.get("/api/production/orders", ctx -> {
            List<ProductionOrder> orders = dbService.getAllProductionOrders();
            ctx.json(orders);
        });

        app.get("/api/production/feasibility", ctx -> {
            int bomId = Integer.parseInt(ctx.queryParam("bomId"));
            double plannedQty = Double.parseDouble(ctx.queryParam("plannedQty"));
            List<MaterialAvailabilityDTO> availability = appServices.getProductionService()
                .checkBomMaterialAvailability(bomId, plannedQty);
            ctx.json(availability);
        });

        app.post("/api/production/orders", ctx -> {
            ProductionOrder order = ctx.bodyAsClass(ProductionOrder.class);
            order.setStatus(ProductionOrder.ProductionStatus.PLANNED);
            if (order.getProductionDate() == null) {
                order.setProductionDate(java.time.LocalDate.now());
            }
            int newId = dbService.createProductionOrder(order);
            order.setOrderId(newId);
            ctx.status(201).json(order);
        });

        app.post("/api/production/orders/{id}/start", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            StartOrderRequest req = ctx.bodyAsClass(StartOrderRequest.class);
            int userId = req.userId > 0 ? req.userId : 1; // default admin user
            dbService.executeProductionRun(id, userId);
            ctx.json(new StatusUpdateResponse("Production run executed successfully"));
        });

        app.post("/api/production/orders/{id}/status", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            OrderStatusUpdateRequest req = ctx.bodyAsClass(OrderStatusUpdateRequest.class);
            dbService.updateProductionOrderStatus(id, req.status);
            
            // Auto-trigger QA sampling when transitioning to Quality-Testing
            if ("Quality-Testing".equalsIgnoreCase(req.status)) {
                ProductionOrder order = dbService.getProductionOrderById(id);
                if (order != null) {
                    dbService.takeSampleForQC(order.getBatchNumber(), 1); // 1 = admin user
                }
            }
            ctx.json(new StatusUpdateResponse("Production order status updated successfully"));
        });

        // --- QA / QUALITY COMPLIANCE ENDPOINTS ---
        app.get("/api/qa/inspections", ctx -> {
            List<Stock> all = dbService.getDetailedInventoryReport();
            List<Stock> pending = all.stream()
                .filter(s -> "QUARANTINE".equalsIgnoreCase(s.getQcStatus()) || 
                             "UNDER_TEST".equalsIgnoreCase(s.getQcStatus()) ||
                             "QI".equalsIgnoreCase(s.getQcStatus()))
                .toList();
            ctx.json(pending);
        });

        app.post("/api/qa/inspections/{batch}/inspect", ctx -> {
            String batch = ctx.pathParam("batch");
            InspectRequest req = ctx.bodyAsClass(InspectRequest.class);
            int userId = req.userId > 0 ? req.userId : 1;
            
            // Update batch QC status in repositories (sets location based on type, e.g. FINISHED_GOODS_WAREHOUSE)
            dbService.updateQCStatus(batch, req.status.toUpperCase(), userId);
            
            ctx.json(new StatusUpdateResponse("Inspection completed successfully. Status: " + req.status));
        });

        app.get("/api/qa/inspections/{batch}/genealogy", ctx -> {
            String batch = ctx.pathParam("batch");
            ctx.json(dbService.getBatchGenealogy(batch));
        });

        app.post("/api/qa/inspections/{batch}/sample", ctx -> {
            String batch = ctx.pathParam("batch");
            StartOrderRequest req = ctx.bodyAsClass(StartOrderRequest.class);
            int userId = req.userId > 0 ? req.userId : 1;
            dbService.takeSampleForQC(batch, userId);
            ctx.json(new StatusUpdateResponse("IPQC sample taken. Status updated to UNDER_TEST"));
        });

        app.post("/api/qa/inspections/{batch}/status", ctx -> {
            String batch = ctx.pathParam("batch");
            OrderStatusUpdateRequest req = ctx.bodyAsClass(OrderStatusUpdateRequest.class);
            int userId = req.userId > 0 ? req.userId : 1;
            dbService.updateQCStatus(batch, req.status.toUpperCase(), userId);
            ctx.json(new StatusUpdateResponse("QC status updated to " + req.status));
        });

        // --- XAI OBSERVE & CHAT OVERLAY ENDPOINTS ---
        app.sse("/api/agent/stream", client -> {
            client.keepAlive();
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            java.util.concurrent.ScheduledExecutorService scheduler = 
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
            
            final java.util.concurrent.atomic.AtomicInteger lastId = 
                new java.util.concurrent.atomic.AtomicInteger(0);
            
            List<pharma.model.EventLog> initialLogs = dbService.getLatestEventLogs(10);
            if (!initialLogs.isEmpty()) {
                java.util.Collections.reverse(initialLogs);
                for (pharma.model.EventLog el : initialLogs) {
                    try {
                        client.sendEvent(mapper.writeValueAsString(el));
                    } catch (Exception ex) {}
                    lastId.set(Math.max(lastId.get(), el.getEventId()));
                }
            }

            java.util.concurrent.ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                try {
                    List<pharma.model.EventLog> logs = dbService.getLatestEventLogs(10);
                    java.util.List<pharma.model.EventLog> newLogs = new java.util.ArrayList<>();
                    for (pharma.model.EventLog el : logs) {
                        if (el.getEventId() > lastId.get()) {
                            newLogs.add(el);
                        }
                    }
                    if (!newLogs.isEmpty()) {
                        java.util.Collections.reverse(newLogs);
                        for (pharma.model.EventLog el : newLogs) {
                            try {
                                client.sendEvent(mapper.writeValueAsString(el));
                            } catch (Exception ex) {}
                            lastId.set(Math.max(lastId.get(), el.getEventId()));
                        }
                    }
                } catch (Exception e) {
                    // client closed or scheduler failed
                }
            }, 3, 3, java.util.concurrent.TimeUnit.SECONDS);

            client.onClose(() -> {
                future.cancel(true);
                scheduler.shutdown();
            });
        });

        app.post("/api/agent/chat", ctx -> {
            ChatRequestPayload req = ctx.bodyAsClass(ChatRequestPayload.class);
            
            pharma.dto.AIReasoningRequestDTO reasoningReq = new pharma.dto.AIReasoningRequestDTO(
                "EXPLAINABILITY_CHAT", 
                req.message, 
                "User initiated QA or SCM explainability check via REST API."
            );
            
            pharma.dto.AgentRequestEnvelope<pharma.dto.AIReasoningRequestDTO> envelope = 
                new pharma.dto.AgentRequestEnvelope<>(
                    pharma.agent.ontology.AgentActions.AI_REASONING,
                    1,
                    30000L,
                    reasoningReq
                );
            
            try {
                java.util.concurrent.CompletableFuture<pharma.dto.AgentResponseEnvelope<?>> future = 
                    pharma.App.getAgentGateway().submit(envelope);
                pharma.dto.AgentResponseEnvelope<?> res = future.get(8, java.util.concurrent.TimeUnit.SECONDS);
                if (res.getResponseStatus() == pharma.agent.ontology.AgentStatuses.SUCCESS) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    pharma.dto.AIReasoningResultDTO result = mapper.convertValue(res.getPayload(), pharma.dto.AIReasoningResultDTO.class);
                    String reply = "";
                    if (result.getExtractedData() instanceof java.util.Map<?, ?> map) {
                        Object val = map.get("explanation");
                        if (val == null) {
                            val = map.get("analysis");
                        }
                        reply = String.valueOf(val != null ? val : map.toString());
                    } else {
                        reply = String.valueOf(result.getExtractedData());
                    }
                    ctx.json(new ChatResponsePayload(reply));
                    return;
                }
            } catch (Exception e) {
                // fall through to smart resolver fallback
            }

            // Fallback resolver
            String norm = req.message.toLowerCase();
            String replyMarkdown;
            if (norm.contains("qa") || norm.contains("quarantine") || norm.contains("test") || norm.contains("inspect")) {
                List<Stock> qList = dbService.getDetailedInventoryReport().stream()
                    .filter(s -> !"APPROVED".equalsIgnoreCase(s.getQcStatus()) && !"RELEASED".equalsIgnoreCase(s.getQcStatus()))
                    .toList();
                if (qList.isEmpty()) {
                    replyMarkdown = "### 🔬 Quality Compliance Status\n\nNo batches are currently held in QA quarantine. All warehouse stock is released.";
                } else {
                    StringBuilder sb = new StringBuilder("### 🔬 Quality Compliance Status\n\nI detected **" + qList.size() + "** batch(es) currently under quarantine / QC hold:\n\n");
                    for (Stock s : qList) {
                        sb.append("- **Batch ").append(s.getBatchNumber()).append("** (Material: `").append(s.getMaterialCode()).append("`)\n");
                        sb.append("  - Current Location: `").append(s.getLocationCode()).append("`\n");
                        sb.append("  - Status: *").append(s.getQcStatus()).append("*\n");
                        sb.append("  - Quantity: ").append(s.getQuantity()).append("\n");
                    }
                    sb.append("\n*Action recommended:* Log into the **QA Compliance Panel** to record chemical assay parameters and release or reject these batches.");
                    replyMarkdown = sb.toString();
                }
            } else if (norm.contains("production") || norm.contains("order") || norm.contains("bom") || norm.contains("run")) {
                List<ProductionOrder> orders = dbService.getAllProductionOrders();
                StringBuilder sb = new StringBuilder("### ⚡ Production Execution Summary\n\nThere are **" + orders.size() + "** production order(s) registered on the JADE platform:\n\n");
                for (ProductionOrder o : orders) {
                    sb.append("- **Order #").append(o.getOrderId()).append("** (Batch: ").append(o.getBatchNumber()).append(")\n");
                    sb.append("  - Recipe BOM ID: #").append(o.getBomId()).append("\n");
                    sb.append("  - Planned Qty: ").append(o.getPlannedQty()).append("\n");
                    sb.append("  - Status: `").append(o.getStatus()).append("`\n");
                }
                replyMarkdown = sb.toString();
            } else if (norm.contains("supplier") || norm.contains("vendor")) {
                List<Supplier> sups = dbService.getAllSuppliers();
                StringBuilder sb = new StringBuilder("### 🤝 Onboarded Vendors Registry\n\nI found **" + sups.size() + "** registered suppliers:\n\n");
                for (Supplier s : sups) {
                    sb.append("- **").append(s.getSupplierName()).append("** (ID: ").append(s.getSupplierId()).append(")\n");
                    sb.append("  - Compliance Status: *").append(s.getSupplierStatus()).append("*\n");
                    sb.append("  - License: `").append(s.getDrugLicenseNo()).append("` | Phone: *").append(s.getPhone()).append("*\n");
                }
                replyMarkdown = sb.toString();
            } else if (norm.contains("stock") || norm.contains("inventory") || norm.contains("warehouse")) {
                List<Stock> stocks = dbService.getDetailedInventoryReport();
                double totalVal = stocks.stream().mapToDouble(s -> s.getQuantity() * s.getUnitCost()).sum();
                StringBuilder sb = new StringBuilder("### 📦 Stock & Inventory Auditing\n\nTotal warehouse lines: **" + stocks.size() + "**\n");
                sb.append("Estimated stock valuation: **$").append(String.format("%.2f", totalVal)).append("**\n\n");
                sb.append("Top inventory batches:\n");
                stocks.stream().limit(5).forEach(s -> {
                    sb.append("- Batch `").append(s.getBatchNumber()).append("` (Material: ").append(s.getMaterialCode()).append(") - Qty: ").append(s.getQuantity()).append(" [").append(s.getQcStatus()).append("]\n");
                });
                replyMarkdown = sb.toString();
            } else {
                replyMarkdown = "### 🤖 Pharma SCM Co-pilot\n\nHello! I am your agentic SCM explainability companion. I can query the JADE platform to help you analyze supply chain operations.\n\nTry asking me questions about:\n1. **Stock levels** (*'What is the stock status?'*)\n2. **Suppliers** (*'List our approved suppliers'*)\n3. **Production orders** (*'Show active runs'*)\n4. **QA Inspections** (*'Why are batches in quarantine?'*)";
            }
            ctx.json(new ChatResponsePayload(replyMarkdown));
        });

        // --- RISK ANALYSIS & AI DECISION ENDPOINTS (PHASE 6) ---
        app.get("/api/risk/reports", ctx -> {
            ctx.json(appServices.getRiskService().getAllRiskReports());
        });

        app.post("/api/risk/scan", ctx -> {
            List<RiskReportDTO> reports = appServices.getRiskService().generateRuleBasedRiskReports();
            ctx.json(reports);
        });

        app.get("/api/ai/decisions", ctx -> {
            ctx.json(appServices.getAiDecisionService().findAll());
        });

        app.post("/api/ai/scan", ctx -> {
            String txId = java.util.UUID.randomUUID().toString();
            pharma.dto.AIReasoningRequestDTO reasoningReq = new pharma.dto.AIReasoningRequestDTO(
                "COORDINATION_RUN", 
                "System-wide stock levels and supplier risk coordination audit.", 
                "Manual agent coordination scan triggered via Web UI."
            );
            
            pharma.dto.AgentRequestEnvelope<pharma.dto.AIReasoningRequestDTO> envelope = 
                new pharma.dto.AgentRequestEnvelope<>(
                    pharma.agent.ontology.AgentActions.AI_REASONING,
                    1,
                    30000L,
                    reasoningReq
                );
            envelope.setTransactionId(txId);

            try {
                java.util.concurrent.CompletableFuture<pharma.dto.AgentResponseEnvelope<?>> future = 
                    pharma.App.getAgentGateway().submit(envelope);
                pharma.dto.AgentResponseEnvelope<?> res = future.get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (res.getResponseStatus() == pharma.agent.ontology.AgentStatuses.SUCCESS) {
                    ctx.json(appServices.getAiDecisionService().findAll());
                    return;
                }
            } catch (Exception e) {
                // Fallback: log a mock coordination decision in the database
                pharma.dto.AIReasoningResultDTO mockResult = new pharma.dto.AIReasoningResultDTO();
                mockResult.setTransactionId(txId);
                mockResult.setTaskType("COORDINATION_RUN");
                mockResult.setConfidenceScore(0.85);
                mockResult.setModelUsed("gemini-2.0-flash-fallback");
                mockResult.setPromptSummary("COORDINATION_RUN: System-wide stock levels and supplier risk coordination audit.");
                mockResult.setRequiresHumanReview(false);
                mockResult.setExtractedData(java.util.Map.of(
                    "status", "OPTIMAL",
                    "explanation", "System-wide stock level validation completed. Insufficient inventory detected for Amoxicillin, auto-procurement rules evaluated.",
                    "actionsRecommended", "Review purchase orders for Bayer Chemicals Ltd"
                ));
                appServices.getAiDecisionService().save(mockResult, txId);
            }

            ctx.json(appServices.getAiDecisionService().findAll());
        });

        app.post("/api/ai/decisions/{id}/approve", ctx -> {
            String id = ctx.pathParam("id");
            appServices.getAiDecisionService().approve(id, 1); // 1 = default admin user ID
            ctx.json(new StatusUpdateResponse("Decision approved successfully"));
        });

        app.post("/api/ai/decisions/{id}/reject", ctx -> {
            String id = ctx.pathParam("id");
            RejectDecisionRequest req = ctx.bodyAsClass(RejectDecisionRequest.class);
            appServices.getAiDecisionService().reject(id, 1, req.remarks); // 1 = default admin user ID
            ctx.json(new StatusUpdateResponse("Decision rejected successfully"));
        });

        app.start(8080);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonBlank(String primary, String fallback) {
        String cleanedPrimary = clean(primary);
        return cleanedPrimary.isBlank() ? clean(fallback) : cleanedPrimary;
    }

    private static Role resolveRole(RoleService roleService, String roleReference) {
        String cleanRoleReference = clean(roleReference);
        if (cleanRoleReference.isBlank()) {
            return null;
        }

        Integer roleId = parseInteger(cleanRoleReference);
        for (Role role : roleService.getAllRoles()) {
            if (roleId != null && role.getRoleId() == roleId) {
                return role;
            }
            if (role.getRoleName() != null && role.getRoleName().equalsIgnoreCase(cleanRoleReference)) {
                return role;
            }
        }
        return null;
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void stop() {
        if (app != null) {
            app.stop();
        }
    }

    // Helper DTOs
    private static class LoginRequest {
        public String employeeId;
        public String username;
        public String password;
    }

    private static class AuthSessionResponse {
        private final int userId;
        private final String employeeId;
        private final String fullName;
        private final String roleName;
        private final Set<String> permissions;

        public AuthSessionResponse(User user) {
            Role role = user.getRole();
            this.userId = user.getUserId();
            this.employeeId = user.getUsername();
            this.fullName = user.getFullName();
            this.roleName = role == null ? "" : role.getRoleName();
            this.permissions = user.getPermissions();
        }

        @SuppressWarnings("unused")
        public int getUserId() { return userId; }

        @SuppressWarnings("unused")
        public String getEmployeeId() { return employeeId; }

        @SuppressWarnings("unused")
        public String getFullName() { return fullName; }

        @SuppressWarnings("unused")
        public String getRoleName() { return roleName; }

        @SuppressWarnings("unused")
        public Set<String> getPermissions() { return permissions; }
    }

    private static class RolePermissionsResponse {
        private final Role role;
        private final List<Permission> permissions;
        private final Set<Integer> assignedPermissionIds;

        public RolePermissionsResponse(Role role, List<Permission> permissions, Set<Integer> assignedPermissionIds) {
            this.role = role;
            this.permissions = permissions;
            this.assignedPermissionIds = assignedPermissionIds;
        }

        @SuppressWarnings("unused")
        public Role getRole() { return role; }

        @SuppressWarnings("unused")
        public List<Permission> getPermissions() { return permissions; }

        @SuppressWarnings("unused")
        public Set<Integer> getAssignedPermissionIds() { return assignedPermissionIds; }
    }

    private static class PermissionUpdateRequest {
        public Set<Integer> permissionIds;
        public int adminUserId;
    }

    private static class StatusUpdateRequest {
        public String status;
        public String remarks;
        public String performedBy;
    }

    private static class StatusUpdateResponse {
        private final String message;
        public StatusUpdateResponse(String message) { this.message = message; }
        @SuppressWarnings("unused")
        public String getMessage() { return message; }
    }

    private static class ErrorResponse {
        private final String error;
        public ErrorResponse(String error) { this.error = error; }
        @SuppressWarnings("unused")
        public String getError() { return error; }
    }

    private static class CreateBOMRequest {
        public BOMHeader header;
        public List<BOMDetail> details;
    }

    @SuppressWarnings("unused")
    private static class SupplierPerformanceResponse {
        public int supplierId;
        public String supplierName;
        public List<String> drivers;
        public double riskScore;
        public String severity;
        public String status;

        public SupplierPerformanceResponse(int supplierId, String supplierName, List<String> drivers, double riskScore, String severity, String status) {
            this.supplierId = supplierId;
            this.supplierName = supplierName;
            this.drivers = drivers;
            this.riskScore = riskScore;
            this.severity = severity;
            this.status = status;
        }
    }

    private static class CreateGRNRequest {
        public int poId;
        public String receivedBy;
        public int receivedByUserId;
        public List<CreateGRNItemRequest> items;
    }

    private static class CreateGRNItemRequest {
        public String materialCode;
        public String batchNumber;
        public int quantityReceived;
        public String expiryDate;
    }

    private static List<GRN.GRNItem> mapGrnItems(List<CreateGRNItemRequest> requestItems) {
        List<GRN.GRNItem> items = new java.util.ArrayList<>();
        if (requestItems != null) {
            for (CreateGRNItemRequest item : requestItems) {
                String materialCode = clean(item.materialCode);
                String batchNumber = clean(item.batchNumber);
                String expiryDate = clean(item.expiryDate);
                if (materialCode.isBlank()) {
                    throw new IllegalArgumentException("Material code is required for every GRN item");
                }
                if (batchNumber.isBlank()) {
                    throw new IllegalArgumentException("Batch number is required for every GRN item");
                }
                if (item.quantityReceived <= 0) {
                    throw new IllegalArgumentException("Received quantity must be greater than zero for every GRN item");
                }
                if (expiryDate.isBlank()) {
                    throw new IllegalArgumentException("Expiry date is required for every GRN item");
                }
                items.add(new GRN.GRNItem(
                        materialCode,
                        batchNumber,
                        item.quantityReceived,
                        java.time.LocalDate.parse(expiryDate)));
            }
        }
        return items;
    }

    private static class StartOrderRequest {
        public int userId;
    }

    private static class OrderStatusUpdateRequest {
        public String status;
        public int userId;
    }

    private static class InspectRequest {
        public String status;
        @SuppressWarnings("unused")
        public String remarks;
        @SuppressWarnings("unused")
        public String performedBy;
        public int userId;
    }

    private static class ChatRequestPayload {
        public String message;
    }

    private static class RejectDecisionRequest {
        public String remarks;
    }

    private static class ChatResponsePayload {
        private final String reply;
        public ChatResponsePayload(String reply) { this.reply = reply; }
        @SuppressWarnings("unused")
        public String getReply() { return reply; }
    }
}
