# Performance Optimization Guide for LangChain4j on Elastic Beanstalk

## Overview

This guide explains the performance optimizations implemented through the `.ebextensions` configuration files for running a LangChain4j AI Agent on AWS Elastic Beanstalk t3.micro instances.

## Optimization Categories

### 1. JVM Optimization (`04-jvm-optimization.config`)

**Key Optimizations:**
- **G1GC Garbage Collector**: Low-latency GC with 200ms pause target
- **Memory Settings**: 512MB heap (50% of t3.micro's 1GB RAM)
- **String Deduplication**: Reduces memory usage for duplicate strings
- **Tiered Compilation**: Faster startup with Level 1 optimization
- **Compressed OOPs**: Reduces memory footprint

**Benefits:**
- Reduced GC pauses for better response times
- Optimal memory utilization for t3.micro
- Faster application startup

### 2. Performance Monitoring (`05-performance-monitoring.config`)

**Features:**
- CloudWatch integration for metrics
- JVM metrics collection (CPU, memory, threads)
- Custom application metrics
- Automated log aggregation

**Key Metrics Tracked:**
- Java process CPU and memory usage
- Thread count and GC statistics
- Network connections
- Application-specific performance data

### 3. System Optimization (`06-system-optimization.config`)

**Kernel Tuning:**
- TCP optimization with BBR congestion control
- Increased file descriptors (65536)
- Memory management (vm.swappiness=1)
- Network buffer optimization

**Resource Management:**
- 512MB swap file for memory pressure
- CPU governor set to performance mode
- SSD-optimized disk scheduler

### 4. Health Check Optimization (`07-health-check-optimization.config`)

**Features:**
- Lightweight health endpoint
- Automatic recovery mechanisms
- Health monitoring with corrective actions
- Fallback health check server

**Benefits:**
- Quick detection of issues
- Automatic application restart on failure
- Reduced false positives

### 5. Nginx Optimization (`08-nginx-optimization.config`)

**Performance Features:**
- Worker process auto-tuning
- Connection pooling with keepalive
- Gzip compression for responses
- Rate limiting for API protection
- Optimized buffer sizes

**Security:**
- Security headers (X-Frame-Options, etc.)
- Request size limits
- Hidden file protection

### 6. Startup Optimization (`09-startup-optimization.config`)

**Startup Acceleration:**
- AppCDS (Application Class Data Sharing)
- JVM pre-warming
- DNS pre-resolution
- Connection pool pre-warming

**Benefits:**
- Faster cold starts
- Reduced time to first response
- Pre-warmed Anthropic API connections

## Performance Tuning Parameters

### Memory Allocation (t3.micro - 1GB RAM)

```
Application Heap: 512MB (50%)
Metaspace: 128MB (12%)
Reserved Code Cache: 64MB
System/OS: 256MB
Swap: 512MB (emergency)
```

### Key JVM Flags

```bash
-XX:+UseG1GC                    # Low-latency garbage collector
-XX:MaxGCPauseMillis=200       # Target GC pause time
-XX:+UseStringDeduplication    # Reduce string memory usage
-XX:+TieredCompilation         # Faster startup
-XX:TieredStopAtLevel=1        # Quick compilation
-XX:+AlwaysPreTouch            # Pre-allocate memory pages
```

### Network Optimization

```bash
net.ipv4.tcp_congestion_control = bbr  # Modern congestion control
net.core.somaxconn = 8192              # Higher connection backlog
net.ipv4.tcp_fastopen = 3              # Faster TCP connections
```

## Monitoring and Alerts

### CloudWatch Metrics

Custom namespace: `LangChain4j-AI-Agent`

**Application Metrics:**
- `JAVA_CPU_USAGE`: Java process CPU percentage
- `JAVA_MEMORY_RSS`: Resident memory size
- `JAVA_THREADS`: Active thread count

**System Metrics:**
- `CPU_USAGE_ACTIVE`: Overall CPU usage
- `MEM_USED_PERCENT`: Memory utilization
- `TCP_ESTABLISHED`: Active connections

### Log Aggregation

**Log Locations:**
- Application logs: `/var/log/eb-app/application.log`
- GC logs: `/var/log/eb-app/gc.log`
- Health monitor: `/var/log/eb-app/health-monitor.log`
- Performance metrics: `/var/log/eb-app/app-metrics.log`

## Best Practices

### 1. Memory Management
- Monitor heap usage via CloudWatch
- Adjust Xmx/Xms based on actual usage
- Use swap only for emergencies

### 2. Connection Management
- Limit concurrent connections in nginx
- Use connection pooling for Anthropic API
- Monitor connection metrics

### 3. Startup Performance
- Keep JAR size minimal
- Use AppCDS for faster restarts
- Implement proper warmup logic

### 4. Monitoring
- Set up CloudWatch alarms for key metrics
- Review GC logs regularly
- Monitor response times

## Troubleshooting

### High Memory Usage
1. Check GC logs for frequency
2. Review heap dumps if OOM occurs
3. Consider upgrading instance type

### Slow Response Times
1. Check CPU usage in CloudWatch
2. Review thread dumps
3. Monitor Anthropic API latency

### Application Crashes
1. Check `/var/log/eb-app/hs_err_pid*.log`
2. Review CloudWatch logs
3. Verify health check responses

## Cost Optimization

These configurations are optimized for t3.micro (free tier):
- Single instance deployment
- Minimal CloudWatch log retention (7 days)
- Efficient resource utilization
- Swap file for memory pressure (avoid upgrades)

## Testing Performance

### Load Testing
```bash
# Simple load test
ab -n 1000 -c 10 http://your-app.elasticbeanstalk.com/health

# Monitor during test
watch -n 1 'curl -s http://your-app.elasticbeanstalk.com/health | jq'
```

### JVM Monitoring
```bash
# SSH into instance
eb ssh

# Check JVM stats
jstat -gc $(pgrep -f application.jar) 1000

# Thread dump
jstack $(pgrep -f application.jar)
```

## Future Optimizations

Consider these for production:
1. **CDN Integration**: CloudFront for static assets
2. **Database Connection Pooling**: If using RDS
3. **Horizontal Scaling**: Auto-scaling groups
4. **Premium Instance Types**: For higher load
5. **ElastiCache**: For session/cache management