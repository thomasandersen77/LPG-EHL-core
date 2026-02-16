# Refactor Azure Config

The Azure configuration (AzureConfig class) should be in the service module.

At the moment, this class is almost identical, and it's in two of them: one in the web app module and one in the headless app. 

Refactor it to be in the service module, so it's shared between the two; webapp and headless.

## Steps

1. Move the AzureConfig class to the service module.
2. Update the tests.
3. Run the tests.
4. Update the reference in the web app module and headless app module.
5. Make sure the tests still pass

# Rules of Refactoring
1. Move the code without changing the functionality.
2. Update the references. And clean the code. 
3. Make sure the web app or the headless app talks to an interface that is created by Spring in the service module. 
4. AzureSyncService is in the service module and should be abstracted away from the web app and the headless app.
5. Important: don't make unnecessary changes to the code. Keep the complexity low.

# Accepted criteria

1. It should work in the same way as it has until now. 
2. The code should be clean.
3. The code should be documented. 
4. The tests should pass.