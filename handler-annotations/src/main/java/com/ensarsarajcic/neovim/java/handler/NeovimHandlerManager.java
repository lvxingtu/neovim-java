/*
 * MIT License
 *
 * Copyright (c) 2018 Ensar Sarajčić
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ensarsarajcic.neovim.java.handler;

import com.ensarsarajcic.neovim.java.corerpc.client.RpcListener;
import com.ensarsarajcic.neovim.java.corerpc.client.RpcStreamer;
import com.ensarsarajcic.neovim.java.corerpc.message.ResponseMessage;
import com.ensarsarajcic.neovim.java.handler.annotations.NeovimNotificationHandler;
import com.ensarsarajcic.neovim.java.handler.annotations.NeovimRequestHandler;
import com.ensarsarajcic.neovim.java.handler.annotations.NeovimRequestListener;
import com.ensarsarajcic.neovim.java.handler.errors.NeovimHandlerException;
import com.ensarsarajcic.neovim.java.handler.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple class used to register and connect {@link NeovimNotificationHandler} and {@link NeovimRequestHandler}
 * It provides methods to register/unregister any kind of object as neovim handler, but only classes with methods
 * annotated with {@link NeovimNotificationHandler} or {@link NeovimRequestHandler} can be useful, since this will
 * find such methods and call them once notifications/requests arrive
 * <p>
 * To get notifications/requests it should first be attached to a {@link RpcStreamer} using {@link #attachToStream(RpcStreamer)}
 * Underneath, {@link NeovimHandlerProxy} is used to dispatch notifications/requests, which delegates thread management to it
 * <p>
 * Examples:
 * <pre>
 *     {@code
 *     NeovimHandlerManager neovimHandlerManager = new NeovimHandlerManager();
 *
 *     neovimHandlerManager.registerNeovimHandler(uiEventHandler);
 *     neovimHandlerManager.attachToStream(neovimStream);
 *     }
 * </pre>
 */
public final class NeovimHandlerManager {
    private static final Logger log = LoggerFactory.getLogger(NeovimHandlerManager.class);

    private NeovimHandlerProxy neovimHandlerProxy;
    private Map<Object, Map.Entry<List<RpcListener.NotificationCallback>, List<RpcListener.RequestCallback>>> handlers = new HashMap<>();
    private WeakReference<RpcStreamer> rpcStreamer;

    /**
     * Creates a new {@link NeovimHandlerManager} with default {@link NeovimHandlerProxy} using {@link ImmediateExecutorService}
     */
    public NeovimHandlerManager() {
        this(new NeovimHandlerProxy());
    }

    /**
     * Creates a new {@link NeovimHandlerManager} with given {@link NeovimHandlerProxy}
     * @param neovimHandlerProxy proxy to use for dispatching notifications/messages
     * @throws NullPointerException if passed {@link NeovimHandlerProxy} is null
     */
    public NeovimHandlerManager(NeovimHandlerProxy neovimHandlerProxy) {
        Objects.requireNonNull(neovimHandlerProxy, "neovimHandlerProxy is required to dispatch notifications/messages");
        this.neovimHandlerProxy = neovimHandlerProxy;
    }

    /**
     * Attaches to given {@link RpcStreamer}
     * {@link RpcStreamer} does not have to be attached to an actual connection at the time of this call, since this
     * only sets up the notification/request callbacks
     *
     * @param rpcStreamer streamer whose notifications/requests should be analysed
     * @throws NullPointerException if {@link RpcStreamer} is null
     */
    public void attachToStream(RpcStreamer rpcStreamer) {
        log.info("Attaching handler manager to streamer ({})", rpcStreamer);
        Objects.requireNonNull(rpcStreamer, "rpcStreamer may not be null");
        rpcStreamer.addNotificationCallback(neovimHandlerProxy);
        rpcStreamer.addRequestCallback(neovimHandlerProxy);
        this.rpcStreamer = new WeakReference<>(rpcStreamer);
    }

    /**
     * Registers a new handler
     * If this object is already registered, this is a no-op
     * If this object has methods annotated with {@link NeovimNotificationHandler} or {@link NeovimRequestHandler},
     * this method will have no effect, but it will still reflectively look for such methods
     * <p>
     * This works for both static and instance methods
     * <p>
     * Passed objects methods are prepared to be called when new notifications/requests arrive from attached {@link RpcStreamer}
     * This may be called prior to attaching, but no notifications/requests can arrive before attaching
     *
     * @param handler object to search for annotated methods
     */
    public void registerNeovimHandler(Object handler) {
        log.info("New neovim handler registered ({})", handler);
        if (handlers.containsKey(handler)) {
            return;
        }

        List<Map.Entry<Method, NeovimNotificationHandler>> notificationHandlers =
                ReflectionUtils.getMethodsAnnotatedWith(handler.getClass(), NeovimNotificationHandler.class);
        List<Map.Entry<Method, NeovimRequestListener>> requestListeners =
                ReflectionUtils.getMethodsAnnotatedWith(handler.getClass(), NeovimRequestListener.class);
        List<Map.Entry<Method, NeovimRequestHandler>> requestHandlers =
                ReflectionUtils.getMethodsAnnotatedWith(handler.getClass(), NeovimRequestHandler.class);

        handlers.put(handler, new AbstractMap.SimpleEntry<>(new ArrayList<>(), new ArrayList<>()));

        List<RpcListener.NotificationCallback> notificationCallbacks = notificationHandlers.stream()
                .map(methodNeovimNotificationHandlerEntry -> {
                    String notificationName = methodNeovimNotificationHandlerEntry.getValue().value();
                    return (RpcListener.NotificationCallback) notificationMessage -> {
                        if (notificationName.equals(notificationMessage.getName())) {
                            try {
                                if (Modifier.isStatic(methodNeovimNotificationHandlerEntry.getKey().getModifiers())) {
                                    methodNeovimNotificationHandlerEntry.getKey().invoke(null, notificationMessage);
                                } else {
                                    methodNeovimNotificationHandlerEntry.getKey().invoke(handler, notificationMessage);
                                }
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                log.error("Error ocurred while invoking handler for notification: " + notificationName, e);
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            }
                        }
                    };
                })
                .collect(Collectors.toList());

        notificationCallbacks.forEach(neovimHandlerProxy::addNotificationCallback);
        handlers.get(handler).getKey().addAll(notificationCallbacks);

        List<RpcListener.RequestCallback> requestCallbacks = requestHandlers.stream()
                .map(methodNeovimRequestHandlerEntry -> {
                    String requestName = methodNeovimRequestHandlerEntry.getValue().value();
                    return (RpcListener.RequestCallback) requestMessage -> {
                        if (requestName.equals(requestMessage.getMethod())) {
                            try {
                                Object result = null;
                                NeovimHandlerException error = null;
                                try {
                                    if (Modifier.isStatic(methodNeovimRequestHandlerEntry.getKey().getModifiers())) {
                                        result = methodNeovimRequestHandlerEntry.getKey().invoke(null, requestMessage);
                                    } else {
                                        result = methodNeovimRequestHandlerEntry.getKey().invoke(handler, requestMessage);
                                    }
                                } catch (InvocationTargetException ex) {
                                    if (ex.getCause() instanceof NeovimHandlerException) {
                                        error = (NeovimHandlerException) ex.getCause();
                                    } else {
                                        throw ex;
                                    }
                                }
                                var streamer = rpcStreamer.get();
                                if (streamer != null) {
                                    streamer.send(new ResponseMessage(requestMessage.getId(), error != null ? error.toRpcError() : null, result));
                                }
                            } catch (IllegalAccessException | InvocationTargetException | IOException e) {
                                log.error("Error ocurred while invoking handler for request: " + requestName, e);
                                e.printStackTrace();
//                                throw new RuntimeException(e);
                            }
                        }
                    };
                })
                .collect(Collectors.toList());

        requestCallbacks.forEach(neovimHandlerProxy::addRequestCallback);
        handlers.get(handler).getValue().addAll(requestCallbacks);

        List<RpcListener.RequestCallback> requestListenersCallbacks = requestListeners.stream()
                .map(methodNeovimRequestListenerEntry -> {
                    String requestName = methodNeovimRequestListenerEntry.getValue().value();
                    return (RpcListener.RequestCallback) requestMessage -> {
                        if (requestName.equals(requestMessage.getMethod())) {
                            try {
                                if (Modifier.isStatic(methodNeovimRequestListenerEntry.getKey().getModifiers())) {
                                    methodNeovimRequestListenerEntry.getKey().invoke(null, requestMessage);
                                } else {
                                    methodNeovimRequestListenerEntry.getKey().invoke(handler, requestMessage);
                                }
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                log.error("Error ocurred while invoking request listener for request: " + requestName, e);
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            }
                        }
                    };
                })
                .collect(Collectors.toList());

        requestListenersCallbacks.forEach(neovimHandlerProxy::addRequestCallback);
        handlers.get(handler).getValue().addAll(requestListenersCallbacks);
    }

    /**
     * Unregisters passed handler
     * If it was not registered previously, this is a no-op
     * It will no longer receive notifications/requests after this point, unless it is registered again
     *
     * @param handler object to unregister
     */
    public void unregisterNeovimHandler(Object handler) {
        log.info("Neovim handler unregistered ({})", handler);
        var callbacks = handlers.remove(handler);
        callbacks.getKey().forEach(neovimHandlerProxy::removeNotificationCallback);
        callbacks.getValue().forEach(neovimHandlerProxy::removeRequestCallback);
    }
}
