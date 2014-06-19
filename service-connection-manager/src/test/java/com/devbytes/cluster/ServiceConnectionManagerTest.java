
package com.devbytes.cluster;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.app.Service;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.devbytes.cluster.ServiceConnectionManager.ServiceCommand;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ServiceConnectionManagerTest {

    private ServiceConnectionManager<Service, Object> manager;

    @Before
    public void setUp() {
        Context context = Robolectric.application;

        manager = new ServiceConnectionManager<Service, Object>(context) {

            @Override
            protected Class<Service> getServiceClass() {
                return Service.class;
            }
        };
    }

    @Test
    public void queueCommand() {
        ServiceCommand<Object> command = mock(ServiceCommand.class);
        manager.runCommand(command);
        verifyNoMoreInteractions(command);
    }

    @Test
    public void runCommandImmediately() throws Exception {
        ServiceCommand<Object> command = mock(ServiceCommand.class);
        IBinder binder = connectService();
        manager.runCommand(command);
        verify(command).run(binder);
    }

    @Test
    public void runCommandsInOrder() throws Exception {
        ServiceCommand<Object> command1 = mock(ServiceCommand.class);
        ServiceCommand<Object> command2 = mock(ServiceCommand.class);

        manager.runCommand(command1);
        IBinder binder = connectService();
        manager.runCommand(command2);

        InOrder order = inOrder(command1, command2);
        order.verify(command1).run(binder);
        order.verify(command2).run(binder);
    }

    private IBinder connectService() throws Exception {
        IBinder binder = mock(IBinder.class);
        Field field = ServiceConnectionManager.class.getDeclaredField("mServiceConnection");
        field.setAccessible(true);
        ServiceConnection connection = (ServiceConnection) field.get(manager);
        connection.onServiceConnected(null, binder);

        return binder;
    }
}
