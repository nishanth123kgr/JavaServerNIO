# Java NIO Multi-Protocol Server

A high-performance, non-blocking server framework built with Java NIO, inspired by the internal architecture of Apache Tomcat. Designed to support multiple application-level protocols through a shared, efficient network core.

## Overview

This project provides a production-grade, reusable NIO-based networking layer that cleanly separates low-level socket management from protocol-specific logic. The architecture enables pluggable protocol implementations without modifications to the core networking layer.

### Current Status

- **HTTP/1.1**: Under Development
- **Kafka Protocol**: Planned for future development

## Key Features

- **Non-blocking I/O**: Selector-based event loop for high throughput and low latency
- **Protocol Agnostic**: Clean separation of network, protocol, and application concerns
- **Production-Inspired**: Architecture modeled after Apache Tomcat's NIO Connector
- **Resource Efficient**: ByteBuffer pooling and minimal memory allocation
- **Scalable Threading**: Configurable worker thread pools with lightweight selector threads
- **Back-pressure Aware**: Response queuing with coordinated write handling

## Architecture

The server follows a battle-tested architectural pattern used by production systems like Tomcat:

```
Client Connection
       ↓
Acceptor Thread (accepts connections)
       ↓
Poller Thread (selector-based event loop)
       ↓
SocketProcessor (worker thread pool)
       ↓
Protocol Handler (HTTP, Kafka, custom)
       ↓
Response Output Queue
       ↓
Poller WRITE (non-blocking writes)
```

### Core Components

#### Acceptor
- Listens on server socket for incoming connections
- Configures sockets as non-blocking
- Registers new connections with the Poller

#### Poller
- Manages the Java NIO Selector
- Handles `OP_READ` and `OP_WRITE` events
- Delegates processing to worker threads
- Maintains minimal logic for fast event loop iteration

#### SocketWrapper
- Wraps `SocketChannel` instances
- Maintains per-connection state
- Manages read buffers and response output queues
- Serves as shared state between Poller and SocketProcessor

#### SocketProcessor
- Executes in configurable worker thread pool
- Reads data from socket buffers
- Delegates to protocol-specific handlers
- Enqueues response ByteBuffers for non-blocking writes

#### Protocol Handlers
Protocol implementations are decoupled from the networking layer through a simple interface:

```java
public interface ProtocolHandler {
    void process(ByteBuffer input, ResponseQueue output);
}
```

This design enables support for HTTP, Kafka, or custom binary protocols using the same server core.

## Threading Model

| Component | Threads | Purpose |
|-----------|---------|---------|
| Acceptor | 1 | Accept new connections |
| Poller | 1-N | Handle selector events |
| Workers | Configurable | Process requests and generate responses |

This model ensures scalability while keeping selector threads lightweight and responsive.

## Performance Characteristics

- **Non-blocking I/O**: All network operations use Java NIO selectors
- **Buffer Pooling**: Reusable ByteBuffer pools minimize GC pressure
- **Batched Writes**: Response queuing enables efficient write coalescing
- **Minimal Locking**: Lock-free data structures where possible

## Inspiration

This project draws architectural inspiration from:

- **Apache Tomcat**: NIO Connector and AbstractProcessor patterns
- **Apache Kafka**: Network layer design and protocol handling
- **High-performance networking**: Non-blocking I/O patterns and buffer management techniques

---

**Note**: This project serves dual purposes as both a practical, reusable server framework and an educational deep-dive into production-grade non-blocking network server implementation.
