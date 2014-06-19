
package com.devbytes.cluster;

import java.lang.ref.WeakReference;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

/**
 * Manages service connections and allows easy asynchronous service interaction.
 * 
 * @author tperrien
 * @param <S> The service to be created.
 * @param <B> The IBinder that is bound to the service.
 */
public abstract class ServiceConnectionManager<S extends Service, B> {

    /**
     * Listener callback used to receive updates when the service is connected and disconnected.
     * 
     * @param <B> The IBinder type.
     */
    public interface ServiceConnectionListener<B> {

        void onServiceConnected(B service);

        void onServiceDisconnected();
    }

    /**
     * Represents a command that can be executed.
     * 
     * @param <B> The IBinder type.
     */
    public interface ServiceCommand<B> {

        void run(B service);

    }

    /** The context used to bind to the service */
    private Context mContext;

    /** A reference to the IBinder returned when the service is connected */
    private B mService;

    /** A set of connection listeners that can be notified when the service is connected/disconnected */
    private final Set<ServiceConnectionListener<B>> mServiceConnectionListeners = new LinkedHashSet<ServiceConnectionListener<B>>();

    /** The queue of commands to run when the service is created and bound */
    private final Queue<WeakReference<ServiceCommand<B>>> mCommandQueue = new ConcurrentLinkedQueue<WeakReference<ServiceCommand<B>>>();

    /** The handler used to execute queued commands */
    private Handler mHandler = new Handler();

    /**
     * Creates a new manager.
     * 
     * @param context The context used to bind to the service.
     */
    public ServiceConnectionManager(Context context) {
        mContext = context;
    }

    /**
     * Start managing and bind to the service.
     */
    public void start() {
        Intent intent = new Intent(mContext, getServiceClass());
        mContext.bindService(intent, mServiceConnection, getConnectionFlags());
    }

    /**
     * Stop managing and unbind from the service.
     */
    public void stop() {
        mContext.unbindService(mServiceConnection);
    }

    public void addServiceConnectionListener(ServiceConnectionListener<B> listener) {
        mServiceConnectionListeners.add(listener);
    }

    public void removeServiceConnectionListener(ServiceConnectionListener<B> listener) {
        mServiceConnectionListeners.remove(listener);
    }

    /**
     * Run the given command immediately on the main thread if the service is bound, or queue the command and run it in the future.
     * 
     * @param command The command to execute.
     */
    public void runCommand(ServiceCommand<B> command) {
        if (mService != null) {
            runCommandInternal(command);
        } else {
            mCommandQueue.offer(new WeakReference<ServiceCommand<B>>(command));
        }
    }

    public void cancelCommand(ServiceCommand<B> command) {
        mCommandQueue.remove(command);
    }

    /**
     * Get the referenced service.
     * 
     * @return The service, or null if this manager is unbound from the service.
     */
    public B getService() {
        return mService;
    }

    /**
     * Get the flags used to bind to the service.
     * 
     * @return The flags used to bind to the service.
     */
    protected int getConnectionFlags() {
        return Context.BIND_AUTO_CREATE;
    }

    /**
     * Provide the implementing class with a concrete service class that can be created and bound.
     * 
     * @return The service class.
     */
    protected abstract Class<S> getServiceClass();

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = (B) service;

            // Notify service connection listeners
            for (ServiceConnectionListener<B> listener : mServiceConnectionListeners) {
                listener.onServiceConnected(mService);
            }

            // Run pending commands
            WeakReference<ServiceCommand<B>> commandRef = mCommandQueue.poll();
            while (commandRef != null) {
                ServiceCommand<B> command = commandRef.get();
                if (command != null) {
                    runCommandInternal(command);
                }
                commandRef = mCommandQueue.poll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;

            mHandler.removeCallbacksAndMessages(null);

            // Notify service connection listeners
            for (ServiceConnectionListener<B> listener : mServiceConnectionListeners) {
                listener.onServiceDisconnected();
            }
        }
    };

    private void runCommandInternal(final ServiceCommand<B> command) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                command.run(mService);
            }
        });
    }
}
