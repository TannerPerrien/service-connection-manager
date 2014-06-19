Serice Connection Manager
=========================

[![Build Status](https://travis-ci.org/TannerPerrien/service-connection-manager.svg?branch=master)](https://travis-ci.org/TannerPerrien/service-connection-manager)

Use ServiceConnectionManager to manage Service connections in Android, asynchronously issue commands to a Service before it is connected, and receive Service lifecycle callbacks.

Usage
-----
For each service that you want to manage, create an extension of the service manager:

```java
class MyServiceConnectionManager extends ServiceConnectionManager<MyService, MyServiceBinder> {

  @Override
  protected Class<MyService> getServiceClass() {
    return MyService.class;
  }

}
```

Then use it in your Activity or Fragment as follows:

```java
class MainActivity extends Activity {

  private MyServiceConnectionManager manager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Create the manager
    manager = new MyServiceConnectionManager();

    // Start the manager and connect to the service
    manager.start();

    // Run a command as soon as the service is bound
    manager.runCommand(new ServiceCommand<MyServiceBinder>() {

    @Override
    public void run(MyServiceBinder service) {
      // get something from the service
    }

  }
}
```
