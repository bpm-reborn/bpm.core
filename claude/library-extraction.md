# BPM Library Extraction Guide

This document outlines the core functionality in BPM that can be extracted into a standalone API library (`koala.api`). The implementation focuses on platform-agnostic abstractions for networking, serialization, UI rendering, and more.

## 1. Custom Networking System

The BPM codebase implements a robust network layer with the following key components:

### 1.1 Network Registry (`Network.kt`)

The `Network` object forms the core of the custom networking system:

```kotlin
object Network {
    // Holds a map of packet IDs to packet supplier functions
    private val packets = ConcurrentHashMap<Int, PacketSupplier>()
    private val packetTypes = ConcurrentHashMap<KClass<out Packet>, Int>()

    // Registration methods
    fun register(packet: KClass<out Packet>, packetSupplier: PacketSupplier): Network
    inline fun <reified P : Packet> register(noinline packetSupplier: PacketSupplier): Network
    
    // Packet creation methods
    fun new(packetId: Int): Packet?
    fun new(packet: KClass<out Packet>): Packet?
    inline fun <reified P : Packet> new(apply: P.() -> Unit): P
}
```

### 1.2 Packet System

Packets are the core data transport mechanism:

- `Packet` interface: Base for all network packets
- Packet serialization/deserialization using custom Buffer system
- Type-safe packet registration and handling

### 1.3 Client and Server Implementation

- `Client.kt`: Manages connections to the server
- `Server.kt`: Handles multiple client connections
- `Endpoint.kt`: Common interface for network endpoints
- `Listener.kt` & `Worker.kt`: Thread management for async networking

## 2. Custom Serialization Framework

The BPM codebase includes a powerful serialization system:

### 2.1 Serialization Registry (`Serial.kt`)

```kotlin
object Serial {
    // Registry for serializers
    private val serializers: MutableMap<Class<*>, Serialize<*>> = mutableMapOf()
    
    // Registration methods
    fun register(type: Class<*>, serializer: Serialize<*>): Serial
    inline fun <reified T : Any> register(serializer: Serialize<T>): Serial
    
    // Read/Write methods
    inline fun <reified T : Any> write(buffer: Buffer, value: T)
    inline fun <reified T : Any> read(buffer: Buffer): T?
    
    // File I/O methods
    inline fun <reified T : Any> write(path: Path, value: T)
    inline fun <reified T : Any> read(path: Path): T?
}
```

### 2.2 Buffer System (`Buffer.kt`)

A comprehensive buffer implementation for reading/writing binary data:

- Interface with methods for primitive types, strings, UUIDs, vectors
- Implementation using `BufferedArray`
- Extension methods for additional data types

### 2.3 Property Serialization

The `PropertySerializer` handles complex object and property serialization.

## 3. ImGui Integration (UI Framework)

BPM uses the ImGui library for UI rendering, which should be abstracted into a clean API:

### 3.1 Theme System (`ITheme.kt`)

```kotlin
class Theme {
    val themeColors: EnumMap<ThemeColor, Vector4f>
    val themeStyle: EnumMap<ThemeStyle, Any>
    
    // Theme application function
    inline fun themed(crossinline block: () -> Unit)
    
    // Style setters
    fun alpha(alpha: Float): Theme
    fun windowPadding(x: Float, y: Float): Theme
    // ... other style methods
    
    // Color setters
    fun text(r: Float, g: Float, b: Float, a: Float): Theme
    fun button(r: Float, g: Float, b: Float, a: Float): Theme
    // ... other color methods
}
```

### 3.2 GUI Utilities (`gui.kt`)

The GUI utilities provide numerous helper functions for ImGui:

- Input handling (mouse, keyboard)
- Font rendering helpers
- Text input components
- Widget extensions

### 3.3 Docking & Panel System

- DockSpace implementation for flexible UI layouts
- Panel system for content organization
- Window management abstractions

## 4. Memory Management

Custom memory management utilities for efficient operation:

- Buffer allocation and wrapping
- Byte array management
- Memory-mapped file utilities

## 5. Implementation Strategy for koala.api

To implement the `koala.api` library:

1. **Define API boundaries**:
   - Create platform-agnostic interfaces for all systems
   - Separate Minecraft-specific code from core functionality

2. **Create abstraction layers**:
   - Network: `INetwork`, `IPacket`, `IEndpoint` interfaces
   - Serialization: `ISerial`, `ISerialize<T>`, `IBuffer` interfaces
   - UI: `ITheme`, `IRender`, `IWindow` interfaces

3. **Implement modular architecture**:
   - Core module: Base interfaces and implementations
   - Network module: Packet and networking system
   - Serialization module: Data serialization system
   - UI module: ImGui abstraction layer

4. **Provide extension points**:
   - Registry systems for plugins/addons
   - Event systems for component communication
   - Configuration systems for customization

## 6. Testing Strategy

For comprehensive testing of the koala.api library:

1. Unit tests for individual components
2. Integration tests for component interactions
3. Mock implementations for external dependencies
4. Performance benchmarks for critical paths

## 7. Documentation Requirements

Each API component should include:

- Interface documentation with usage examples
- Implementation notes for platform-specific concerns
- Extension guidance for adding new functionality
- API versioning and compatibility notes

By extracting these components into a clean, well-documented API, the `koala.api` library will provide a solid foundation for building modular, extensible applications within the Minecraft ecosystem and beyond.