# Codex Handover Context: Pharma SCM Web UI Refinement

## Project Overview
This project is an **Agentic Pharma Supply Chain Management (SCM)** system ("Pharma IMS"). 
We are currently upgrading from a legacy Swing desktop application (pre-v1) to a modern Web UI (v1.1) with a Java Javalin REST API backend.

**Goal**: Achieve full feature parity with the legacy Swing application in the new Web UI without breaking the existing backend code, while simultaneously applying a modern, premium design upgrade.

## Current State & Work Accomplished
1. **Legacy Swing UI Mapping**: We have successfully analyzed all screenshots of the legacy Swing application. It consisted of 13 main sidebar modules: Login, Materials, Inventory, Locations, Suppliers, Purchase Orders, GRN, Production, Quality, Reports, Risk Dashboard, AI Decision, and Admin (RBAC).
2. **Current Web UI Audit**: We audited the current state of the Web UI (`web-ui/src/main.js`, `web-ui/index.html`) and the backend API (`pharma.api.ApiServer`).
3. **Gap Analysis Completed**: We identified that 6 key modules are completely missing from the current Web UI (Login, Locations, Purchase Orders, GRN, Reports, Admin RBAC), and 2 modules need functional enhancements (Production, Quality Dashboard) to match the Swing legacy features.
4. **Backend Readiness**: The `DatabaseService.java` backend already supports almost all the required functionality (e.g., location CRUD, PO creation, receiving shipments, RBAC validation, user auth). We just need to expose the corresponding endpoints in `ApiServer.java` and build the frontend views.

## The Implementation Plan
An 8-phase implementation plan has been drafted and saved in `docs/v1.1_web_ui_refinement_plan.md`. It covers:
- **Phase 1**: Login Screen & Authentication (Enforcing login gate)
- **Phase 2**: Missing Sidebar Modules (Locations, Purchase Orders, GRN)
- **Phase 3**: Reports Module (5 standard reports exporting to CSV)
- **Phase 4**: Admin (RBAC) Module (Role/Permissions management grid)
- **Phase 5**: Enhanced Production Module (Shortage analysis, order details)
- **Phase 6**: Enhanced Quality Dashboard (Batch details, traceability, state-driven workflow)
- **Phase 7**: Risk Dashboard & AI Decision Modules
- **Phase 8**: Visual Design Upgrade (Premium aesthetics, glassmorphism, animations)

## Handover Instructions for Codex
As Codex, you are tasked with executing the implementation plan. 
**Crucial constraints to remember:**
1. **Do not break existing code:** The backend architecture and database structure are already robust and handling GMP regulatory requirements (e.g., hard deletes are blocked for certain entities like Suppliers to preserve audit history). Work within the existing `DatabaseService` methods.
2. **Design Quality is Paramount:** The user explicitly wants a *premium*, modern web design (vibrant colors, sleek dark modes, glassmorphism, micro-animations). Avoid generic, basic looks. Use Vanilla CSS in `style.css`.
3. **Phased Approach:** Refer to `docs/v1.1_web_ui_refinement_plan.md` for the exact breakdown of tasks. Start by clarifying the 4 "Open Questions" listed at the bottom of the plan with the user if they haven't been answered yet.

Good luck with the implementation!
