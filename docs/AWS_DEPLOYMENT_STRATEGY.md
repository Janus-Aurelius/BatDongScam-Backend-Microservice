# AWS Deployment Strategy & RAID 10 Database Architecture
**Project:** BatDongScam Backend (Full Microservices Architecture)  
**Author:** Thien An Nguyen  
**Role:** AWS Cloud Architect & Academic Mentor Reference  

---

## 1. Executive Summary: Target Architecture
The system utilizes a **Distributed Microservices Architecture** with a **Database-per-Service** pattern. While the infrastructure is consolidated for cost-efficiency in this academic model, **Logical Isolation** is enforced at the database layer.

*   **Traffic Routing:** AWS API Gateway/WAF routes requests to ECS Fargate services.
*   **Data Isolation:** Each microservice (IAM, Property, Transaction) connects *only* to its own dedicated logical database.
*   **Storage Backbone:** A high-performance **RAID 10** storage pool managed by a PostgreSQL cluster, utilizing asynchronous durability to ensure "Performance without the Burden."

---

## 2. AWS Deployment Architecture (Logical Isolation)
The diagram now shows the **Database-per-Service** implementation where each service has its own private data domain.

```mermaid
architecture-beta
    group vpc(cloud)[VPC - AWS Region]
    
    %% entry
    node gateway(server)[API Gateway / WAF] in vpc
    
    %% Microservices Layer
    group services(server)[Microservices Layer (ECS Fargate)] in vpc
    node iam_s(server)[IAM Service] in services
    node prop_s(server)[Property Service] in services
    node trans_s(server)[Transaction Service] in services
    
    %% DB Layer Logic
    group db_cluster(database)[Managed PostgreSQL Cluster] in vpc
    node db_engine(server)[PostgreSQL Engine] in db_cluster
    
    %% Logical Isolation (Database-per-Service)
    group logical_dbs(database)[Logical DB Isolation] in db_cluster
    node iam_db(database)[IAM_DB] in logical_dbs
    node prop_db(database)[Prop_DB] in logical_dbs
    node trans_db(database)[Trans_DB] in logical_dbs
    
    %% RAID 10 Detail
    group raid10(storage)[RAID 10 Storage Pool] in db_cluster
    node m1_v1(disk)[EBS Vol 1] in raid10
    node m1_v2(disk)[EBS Vol 2] in raid10
    node m2_v3(disk)[EBS Vol 3] in raid10
    node m2_v4(disk)[EBS Vol 4] in raid10
    
    %% Backup Path
    node s3_backups(database)[Amazon S3 (WAL + Snapshots)] in vpc
    
    %% Connections
    iam_s:B -- T:iam_db
    prop_s:B -- T:prop_db
    trans_s:B -- T:trans_db
    
    logical_dbs:B -- T:db_engine
    db_engine:B -- T:raid10
    raid10:R -- L:s3_backups
```

---

## 3. The Backup Strategy: "Durability without the Burden"
To prevent performance degradation (the "Heavy Burden" of backing up on every write), the system uses a **Two-Tier Asynchronous Backup Orchestration**:

### Tier 1: Continuous WAL Archiving (Real-time RPO)
*   **When:** Every time a transaction is committed, it is first written to the **Write-Ahead Log (WAL)** on the RAID 10 array.
*   **The "Burden" Fix:** PostgreSQL asynchronously streams these WAL segments to **Amazon S3** every 60 seconds (or when a log file reaches 16MB). 
*   **Benefit:** This is a sequential append operation (very fast) rather than a full backup, ensuring that the performance of the microservices is never throttled by the backup process.

### Tier 2: Scheduled EBS Snapshots (Point-in-Time RTO)
*   **When:** Once every 24 hours (during low-traffic maintenance windows).
*   **The "Burden" Fix:** EBS snapshots are **incremental** and happen at the block level *below* the filesystem. The DB engine does not pause operations.
*   **Benefit:** Provides a baseline "Safe State" to restore from quickly.

---

## 4. Database-per-Service Implementation
In a true microservice architecture, the **IAM Service cannot query the Transaction Service's tables.** We implement this via:
1.  **Logical Silos:** `iam_db`, `property_db`, and `transaction_db` are separate logical entities.
2.  **User Access Control:** Each microservice has its own unique DB Credentials. The `property_user` cannot see the `iam_db`.
3.  **Shared Storage, Isolated Logic:** While they share the high-performance **RAID 10** disk pool for cost/management efficiency, the **schemas and data remain 100% decoupled.**

---

## 5. Presentation Defense: Architecture Refinement
*Use these points to answer your professor's hard questions:*

*   **"Why share one PostgreSQL engine?":** "While microservices require database isolation, managing 10 separate EC2 instances is an operational nightmare. We use **Logical Database Isolation** where each service has a private database. This satisfies the microservice principle of 'Independent Deployability' while leveraging a single, high-performance RAID 10 cluster for IOPS efficiency."
*   **"How do you handle backup overhead?":** "We do NOT perform full backups during operations. We use **Asynchronous WAL Archiving**. The database writes a tiny log of the change immediately (which is what RAID 10 is built for), and then 'ships' those logs to S3 in the background. This keeps the application latency near zero."
*   **"What happens in a disaster?":** "We restore the latest daily block-level snapshot (Fast Recovery) and then 'Replay' the WAL logs from S3 (Precision Recovery). This gives us a 1-minute RPO without ever slowing down the user's experience."
*   **"How did you choose the backup window?":** "The maintenance window is an **Empirical Decision**. We use **AWS CloudWatch** to monitor traffic patterns over time. By identifying the 'Trough' in our 24-hour cycle, we schedule heavy operations (like Snapshot consolidation) to ensure zero impact on our actual users. We don't guess; we measure."
ground. This keeps the application latency near zero."
*   **"What happens in a disaster?":** "We restore the latest daily block-level snapshot (Fast Recovery) and then 'Replay' the WAL logs from S3 (Precision Recovery). This gives us a 1-minute RPO without ever slowing down the user's experience."
