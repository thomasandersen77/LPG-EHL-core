# Task

Refactor the core and emulator modules and move 3 classes from the core module to the emulator module.  

##  Rules
1. The core module should not know about Spring.
2. The emulator module shall depend on the core module, but not vice versa.
3. The Emulator module shall use the Core module and the EHL protocol.
4. The emulator module shall have Spring annotations and create beans with @Profile("LAB")
5. Dont make any other changes to the code i core module.

# In the core module.

## Description
In the core module, I have the classes:
- DispenserSimulator
- EhlDispenserEmulator
- InMemorySerialPort

1. I want to move these three classes to the emulator module and replace them with the same classes in the emulator module.
2. The classes in the core should be replaced by interfaces, and the classes in the emulator module should implement those interfaces (inversion of control)
3. This will make the core module more independent and easier to test. Separations of concerns are clearly defined this way, the core module should not know about the emulator module.
4. The core module should only be responsible for the EHL protocol. 

## Acceptence criteria
- The classes in the core module are replaced by interfaces, and the classes in the emulator module implement those interfaces as Spring beans with LAB profile.
- All tests pass in the core module 
- The core module should not be affected as a result of this refactoring other than converting the classes to interfaces.

