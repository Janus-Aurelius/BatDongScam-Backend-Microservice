const fastify = require('fastify');
const autocannon = require('autocannon');

// Configuration
const DB_LATENCY_MS = 25;
const QUERY_COMPUTE_MS = 8;  // Simulated CPU time for "Query"
const UPDATE_COMPUTE_MS = 2; // Simulated CPU time for "Update"

/**
 * Simulates a Processor (P1 or P2) with compute-intensive endpoints
 */
async function createProcessor(port, name) {
    const server = fastify();
    let totalCpuTimeMs = 0;

    // Simulate Query Operation (Heaviest Read)
    server.get('/query', async (request, reply) => {
        const start = Date.now();
        console.log(`[${new Date().toISOString()}] Processor ${name} RECEIVED: GET /query`);
        
        // Simulate CPU Work
        while (Date.now() - start < QUERY_COMPUTE_MS) { /* CPU Busy */ }
        totalCpuTimeMs += QUERY_COMPUTE_MS;
        
        // Simulate DB Latency (Singapore Neon)
        await new Promise(resolve => setTimeout(resolve, DB_LATENCY_MS));
        return { status: 'success', data: 'search_results' };
    });

    // Simulate Update Operation (Lighter Write)
    server.post('/update', async (request, reply) => {
        const start = Date.now();
        console.log(`[${new Date().toISOString()}] Processor ${name} RECEIVED: POST /update`);
        
        // Simulate CPU Work
        while (Date.now() - start < UPDATE_COMPUTE_MS) { /* CPU Busy */ }
        totalCpuTimeMs += UPDATE_COMPUTE_MS;
        
        // Simulate DB Latency (Singapore Neon)
        await new Promise(resolve => setTimeout(resolve, DB_LATENCY_MS));
        return { status: 'success', data: 'status_updated' };
    });

    server.get('/metrics', async () => {
        const current = totalCpuTimeMs;
        totalCpuTimeMs = 0; // Reset for next strategy
        return { cpuTimeMs: current };
    });

    await server.listen({ port });
    console.log(`Processor ${name} listening on port ${port}`);
    return server;
}

async function runBenchmark(url, connections, amount, method = 'GET') {
    return new Promise((resolve, reject) => {
        autocannon({
            url,
            connections,
            amount,
            method,
            duration: 5 // Run for 5 seconds
        }, (err, result) => {
            if (err) reject(err);
            else resolve(result);
        });
    });
}

async function start() {
    const p1 = await createProcessor(3001, 'P1');
    const p2 = await createProcessor(3002, 'P2');

    console.log('\n--- EXECUTING STRATEGY 1: CO-LOCATED ---');
    console.log('Routing 100 Updates/sec and 30 Queries/sec ONLY to P1...');
    
    // Total requests = rate * duration (5s)
    await Promise.all([
        runBenchmark('http://localhost:3001/update', 10, 500, 'POST'), // 100 req/sec
        runBenchmark('http://localhost:3001/query', 5, 150, 'GET')     // 30 req/sec
    ]);

    const m1_s1 = await (await fetch('http://localhost:3001/metrics')).json();
    const m2_s1 = await (await fetch('http://localhost:3002/metrics')).json();

    console.log('\n--- EXECUTING STRATEGY 2: DISTRIBUTED/ROUTED ---');
    console.log('Routing 50 Queries/sec to P1, 150 Updates/sec to P2...');

    await Promise.all([
        runBenchmark('http://localhost:3001/query', 5, 250, 'GET'),   // 50 req/sec
        runBenchmark('http://localhost:3002/update', 10, 750, 'POST') // 150 req/sec
    ]);

    const m1_s2 = await (await fetch('http://localhost:3001/metrics')).json();
    const m2_s2 = await (await fetch('http://localhost:3002/metrics')).json();

    printReport(m1_s1.cpuTimeMs / 5, m2_s1.cpuTimeMs / 5, m1_s2.cpuTimeMs / 5, m2_s2.cpuTimeMs / 5);

    await p1.close();
    await p2.close();
}

function printReport(p1_s1, p2_s1, p1_s2, p2_s2) {
    const calcLoad = (ms) => (ms / 10).toFixed(2); // ms per second / 1000ms * 100

    console.log('\n================ ARCHITECTURAL DEPLOYMENT REPORT ================');
    console.log('STRATEGY 1: CO-LOCATED (All traffic to P1)');
    console.log(`  P1 CPU Load: ${calcLoad(p1_s1)}% ${p1_s1 > 500 ? '[FAIL]' : '[PASS]'}`);
    console.log(`  P2 CPU Load: ${calcLoad(p2_s1)}% [PASS]`);

    console.log('\nSTRATEGY 2: DISTRIBUTED (Routed by Op Type)');
    console.log(`  P1 CPU Load: ${calcLoad(p1_s2)}% ${p1_s2 > 500 ? '[FAIL]' : '[PASS]'}`);
    console.log(`  P2 CPU Load: ${calcLoad(p2_s2)}% ${p2_s2 > 500 ? '[FAIL]' : '[PASS]'}`);

    const finalPass = p1_s1 <= 500 && p2_s1 <= 500 && p1_s2 <= 500 && p2_s2 <= 500;
    console.log(`\nOVERALL VERDICT: ${finalPass ? 'PASS' : 'FAIL'}`);
    console.log('=================================================================\n');
}

start().catch(console.error);
