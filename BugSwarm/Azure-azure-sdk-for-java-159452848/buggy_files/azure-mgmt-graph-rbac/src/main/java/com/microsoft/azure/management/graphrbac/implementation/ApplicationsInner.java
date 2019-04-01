/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.graphrbac.implementation;

import retrofit2.Retrofit;
import com.google.common.reflect.TypeToken;
import com.microsoft.azure.AzureServiceResponseBuilder;
import com.microsoft.azure.CloudException;
import com.microsoft.rest.ServiceCall;
import com.microsoft.rest.ServiceCallback;
import com.microsoft.rest.ServiceResponse;
import com.microsoft.rest.ServiceResponseCallback;
import com.microsoft.rest.Validator;
import java.io.IOException;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.HTTP;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.Response;

/**
 * An instance of this class provides access to all the operations defined
 * in Applications.
 */
public final class ApplicationsInner {
    /** The Retrofit service to perform REST calls. */
    private ApplicationsService service;
    /** The service client containing this operation class. */
    private GraphRbacManagementClientImpl client;

    /**
     * Initializes an instance of ApplicationsInner.
     *
     * @param retrofit the Retrofit instance built from a Retrofit Builder.
     * @param client the instance of the service client containing this operation class.
     */
    public ApplicationsInner(Retrofit retrofit, GraphRbacManagementClientImpl client) {
        this.service = retrofit.create(ApplicationsService.class);
        this.client = client;
    }

    /**
     * The interface defining all the services for Applications to be
     * used by Retrofit to perform actually REST calls.
     */
    interface ApplicationsService {
        @Headers("Content-Type: application/json; charset=utf-8")
        @POST("{tenantID}/applications")
        Call<ResponseBody> create(@Path("tenantID") String tenantID, @Body ApplicationCreateParametersInner parameters, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

        @Headers("Content-Type: application/json; charset=utf-8")
        @GET("{tenantID}/applications")
        Call<ResponseBody> list(@Path("tenantID") String tenantID, @Query("$filter") String filter, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

        @Headers("Content-Type: application/json; charset=utf-8")
        @HTTP(path = "{tenantID}/applications/{applicationObjectId}", method = "DELETE", hasBody = true)
        Call<ResponseBody> delete(@Path(value = "applicationObjectId", encoded = true) String applicationObjectId, @Path("tenantID") String tenantID, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

        @Headers("Content-Type: application/json; charset=utf-8")
        @GET("{tenantID}/applications/{applicationObjectId}")
        Call<ResponseBody> get(@Path(value = "applicationObjectId", encoded = true) String applicationObjectId, @Path("tenantID") String tenantID, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

        @Headers("Content-Type: application/json; charset=utf-8")
        @PATCH("{tenantID}/applications/{applicationObjectId}")
        Call<ResponseBody> patch(@Path(value = "applicationObjectId", encoded = true) String applicationObjectId, @Path("tenantID") String tenantID, @Body ApplicationUpdateParametersInner parameters, @Query("api-version") String apiVersion, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

    }

    /**
     * Create a new application. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @param parameters Parameters to create an application.
     * @throws CloudException exception thrown from REST call
     * @throws IOException exception thrown from serialization/deserialization
     * @throws IllegalArgumentException exception thrown from invalid parameters
     * @return the ApplicationInner object wrapped in {@link ServiceResponse} if successful.
     */
    public ServiceResponse<ApplicationInner> create(ApplicationCreateParametersInner parameters) throws CloudException, IOException, IllegalArgumentException {
        if (this.client.tenantID() == null) {
            throw new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null.");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Parameter parameters is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        Validator.validate(parameters);
        Call<ResponseBody> call = service.create(this.client.tenantID(), parameters, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        return createDelegate(call.execute());
    }

    /**
     * Create a new application. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @param parameters Parameters to create an application.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if callback is null
     * @return the {@link Call} object
     */
    public ServiceCall createAsync(ApplicationCreateParametersInner parameters, final ServiceCallback<ApplicationInner> serviceCallback) throws IllegalArgumentException {
        if (serviceCallback == null) {
            throw new IllegalArgumentException("ServiceCallback is required for async calls.");
        }
        if (this.client.tenantID() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null."));
            return null;
        }
        if (parameters == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter parameters is required and cannot be null."));
            return null;
        }
        if (this.client.apiVersion() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null."));
            return null;
        }
        Validator.validate(parameters, serviceCallback);
        Call<ResponseBody> call = service.create(this.client.tenantID(), parameters, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        final ServiceCall serviceCall = new ServiceCall(call);
        call.enqueue(new ServiceResponseCallback<ApplicationInner>(serviceCallback) {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    serviceCallback.success(createDelegate(response));
                } catch (CloudException | IOException exception) {
                    serviceCallback.failure(exception);
                }
            }
        });
        return serviceCall;
    }

    private ServiceResponse<ApplicationInner> createDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return new AzureServiceResponseBuilder<ApplicationInner, CloudException>(this.client.mapperAdapter())
                .register(201, new TypeToken<ApplicationInner>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

    /**
     * Lists applications by filter parameters. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @throws CloudException exception thrown from REST call
     * @throws IOException exception thrown from serialization/deserialization
     * @throws IllegalArgumentException exception thrown from invalid parameters
     * @return the List&lt;ApplicationInner&gt; object wrapped in {@link ServiceResponse} if successful.
     */
    public ServiceResponse<List<ApplicationInner>> list() throws CloudException, IOException, IllegalArgumentException {
        if (this.client.tenantID() == null) {
            throw new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        final String filter = null;
        Call<ResponseBody> call = service.list(this.client.tenantID(), filter, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        ServiceResponse<PageImpl<ApplicationInner>> response = listDelegate(call.execute());
        List<ApplicationInner> result = response.getBody().getItems();
        return new ServiceResponse<>(result, response.getResponse());
    }

    /**
     * Lists applications by filter parameters. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if callback is null
     * @return the {@link Call} object
     */
    public ServiceCall listAsync(final ServiceCallback<List<ApplicationInner>> serviceCallback) throws IllegalArgumentException {
        if (serviceCallback == null) {
            throw new IllegalArgumentException("ServiceCallback is required for async calls.");
        }
        if (this.client.tenantID() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null."));
            return null;
        }
        if (this.client.apiVersion() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null."));
            return null;
        }
        final String filter = null;
        Call<ResponseBody> call = service.list(this.client.tenantID(), filter, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        final ServiceCall serviceCall = new ServiceCall(call);
        call.enqueue(new ServiceResponseCallback<List<ApplicationInner>>(serviceCallback) {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ServiceResponse<PageImpl<ApplicationInner>> result = listDelegate(response);
                    serviceCallback.success(new ServiceResponse<>(result.getBody().getItems(), result.getResponse()));
                } catch (CloudException | IOException exception) {
                    serviceCallback.failure(exception);
                }
            }
        });
        return serviceCall;
    }

    /**
     * Lists applications by filter parameters. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @param filter The filters to apply on the operation
     * @throws CloudException exception thrown from REST call
     * @throws IOException exception thrown from serialization/deserialization
     * @throws IllegalArgumentException exception thrown from invalid parameters
     * @return the List&lt;ApplicationInner&gt; object wrapped in {@link ServiceResponse} if successful.
     */
    public ServiceResponse<List<ApplicationInner>> list(String filter) throws CloudException, IOException, IllegalArgumentException {
        if (this.client.tenantID() == null) {
            throw new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        Call<ResponseBody> call = service.list(this.client.tenantID(), filter, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        ServiceResponse<PageImpl<ApplicationInner>> response = listDelegate(call.execute());
        List<ApplicationInner> result = response.getBody().getItems();
        return new ServiceResponse<>(result, response.getResponse());
    }

    /**
     * Lists applications by filter parameters. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @param filter The filters to apply on the operation
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if callback is null
     * @return the {@link Call} object
     */
    public ServiceCall listAsync(String filter, final ServiceCallback<List<ApplicationInner>> serviceCallback) throws IllegalArgumentException {
        if (serviceCallback == null) {
            throw new IllegalArgumentException("ServiceCallback is required for async calls.");
        }
        if (this.client.tenantID() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null."));
            return null;
        }
        if (this.client.apiVersion() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null."));
            return null;
        }
        Call<ResponseBody> call = service.list(this.client.tenantID(), filter, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        final ServiceCall serviceCall = new ServiceCall(call);
        call.enqueue(new ServiceResponseCallback<List<ApplicationInner>>(serviceCallback) {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    ServiceResponse<PageImpl<ApplicationInner>> result = listDelegate(response);
                    serviceCallback.success(new ServiceResponse<>(result.getBody().getItems(), result.getResponse()));
                } catch (CloudException | IOException exception) {
                    serviceCallback.failure(exception);
                }
            }
        });
        return serviceCall;
    }

    private ServiceResponse<PageImpl<ApplicationInner>> listDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return new AzureServiceResponseBuilder<PageImpl<ApplicationInner>, CloudException>(this.client.mapperAdapter())
                .register(200, new TypeToken<PageImpl<ApplicationInner>>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

    /**
     * Delete an application. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @param applicationObjectId Application object id
     * @throws CloudException exception thrown from REST call
     * @throws IOException exception thrown from serialization/deserialization
     * @throws IllegalArgumentException exception thrown from invalid parameters
     * @return the {@link ServiceResponse} object if successful.
     */
    public ServiceResponse<Void> delete(String applicationObjectId) throws CloudException, IOException, IllegalArgumentException {
        if (applicationObjectId == null) {
            throw new IllegalArgumentException("Parameter applicationObjectId is required and cannot be null.");
        }
        if (this.client.tenantID() == null) {
            throw new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        Call<ResponseBody> call = service.delete(applicationObjectId, this.client.tenantID(), this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        return deleteDelegate(call.execute());
    }

    /**
     * Delete an application. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @param applicationObjectId Application object id
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if callback is null
     * @return the {@link Call} object
     */
    public ServiceCall deleteAsync(String applicationObjectId, final ServiceCallback<Void> serviceCallback) throws IllegalArgumentException {
        if (serviceCallback == null) {
            throw new IllegalArgumentException("ServiceCallback is required for async calls.");
        }
        if (applicationObjectId == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter applicationObjectId is required and cannot be null."));
            return null;
        }
        if (this.client.tenantID() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null."));
            return null;
        }
        if (this.client.apiVersion() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null."));
            return null;
        }
        Call<ResponseBody> call = service.delete(applicationObjectId, this.client.tenantID(), this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        final ServiceCall serviceCall = new ServiceCall(call);
        call.enqueue(new ServiceResponseCallback<Void>(serviceCallback) {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    serviceCallback.success(deleteDelegate(response));
                } catch (CloudException | IOException exception) {
                    serviceCallback.failure(exception);
                }
            }
        });
        return serviceCall;
    }

    private ServiceResponse<Void> deleteDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return new AzureServiceResponseBuilder<Void, CloudException>(this.client.mapperAdapter())
                .register(204, new TypeToken<Void>() { }.getType())
                .build(response);
    }

    /**
     * Get an application by object Id. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @param applicationObjectId Application object id
     * @throws CloudException exception thrown from REST call
     * @throws IOException exception thrown from serialization/deserialization
     * @throws IllegalArgumentException exception thrown from invalid parameters
     * @return the ApplicationInner object wrapped in {@link ServiceResponse} if successful.
     */
    public ServiceResponse<ApplicationInner> get(String applicationObjectId) throws CloudException, IOException, IllegalArgumentException {
        if (applicationObjectId == null) {
            throw new IllegalArgumentException("Parameter applicationObjectId is required and cannot be null.");
        }
        if (this.client.tenantID() == null) {
            throw new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        Call<ResponseBody> call = service.get(applicationObjectId, this.client.tenantID(), this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        return getDelegate(call.execute());
    }

    /**
     * Get an application by object Id. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @param applicationObjectId Application object id
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if callback is null
     * @return the {@link Call} object
     */
    public ServiceCall getAsync(String applicationObjectId, final ServiceCallback<ApplicationInner> serviceCallback) throws IllegalArgumentException {
        if (serviceCallback == null) {
            throw new IllegalArgumentException("ServiceCallback is required for async calls.");
        }
        if (applicationObjectId == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter applicationObjectId is required and cannot be null."));
            return null;
        }
        if (this.client.tenantID() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null."));
            return null;
        }
        if (this.client.apiVersion() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null."));
            return null;
        }
        Call<ResponseBody> call = service.get(applicationObjectId, this.client.tenantID(), this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        final ServiceCall serviceCall = new ServiceCall(call);
        call.enqueue(new ServiceResponseCallback<ApplicationInner>(serviceCallback) {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    serviceCallback.success(getDelegate(response));
                } catch (CloudException | IOException exception) {
                    serviceCallback.failure(exception);
                }
            }
        });
        return serviceCall;
    }

    private ServiceResponse<ApplicationInner> getDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return new AzureServiceResponseBuilder<ApplicationInner, CloudException>(this.client.mapperAdapter())
                .register(200, new TypeToken<ApplicationInner>() { }.getType())
                .registerError(CloudException.class)
                .build(response);
    }

    /**
     * Update existing application. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @param applicationObjectId Application object id
     * @param parameters Parameters to update an existing application.
     * @throws CloudException exception thrown from REST call
     * @throws IOException exception thrown from serialization/deserialization
     * @throws IllegalArgumentException exception thrown from invalid parameters
     * @return the {@link ServiceResponse} object if successful.
     */
    public ServiceResponse<Void> patch(String applicationObjectId, ApplicationUpdateParametersInner parameters) throws CloudException, IOException, IllegalArgumentException {
        if (applicationObjectId == null) {
            throw new IllegalArgumentException("Parameter applicationObjectId is required and cannot be null.");
        }
        if (this.client.tenantID() == null) {
            throw new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null.");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Parameter parameters is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        Validator.validate(parameters);
        Call<ResponseBody> call = service.patch(applicationObjectId, this.client.tenantID(), parameters, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        return patchDelegate(call.execute());
    }

    /**
     * Update existing application. Reference: http://msdn.microsoft.com/en-us/library/azure/hh974476.aspx.
     *
     * @param applicationObjectId Application object id
     * @param parameters Parameters to update an existing application.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if callback is null
     * @return the {@link Call} object
     */
    public ServiceCall patchAsync(String applicationObjectId, ApplicationUpdateParametersInner parameters, final ServiceCallback<Void> serviceCallback) throws IllegalArgumentException {
        if (serviceCallback == null) {
            throw new IllegalArgumentException("ServiceCallback is required for async calls.");
        }
        if (applicationObjectId == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter applicationObjectId is required and cannot be null."));
            return null;
        }
        if (this.client.tenantID() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.tenantID() is required and cannot be null."));
            return null;
        }
        if (parameters == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter parameters is required and cannot be null."));
            return null;
        }
        if (this.client.apiVersion() == null) {
            serviceCallback.failure(new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null."));
            return null;
        }
        Validator.validate(parameters, serviceCallback);
        Call<ResponseBody> call = service.patch(applicationObjectId, this.client.tenantID(), parameters, this.client.apiVersion(), this.client.acceptLanguage(), this.client.userAgent());
        final ServiceCall serviceCall = new ServiceCall(call);
        call.enqueue(new ServiceResponseCallback<Void>(serviceCallback) {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    serviceCallback.success(patchDelegate(response));
                } catch (CloudException | IOException exception) {
                    serviceCallback.failure(exception);
                }
            }
        });
        return serviceCall;
    }

    private ServiceResponse<Void> patchDelegate(Response<ResponseBody> response) throws CloudException, IOException, IllegalArgumentException {
        return new AzureServiceResponseBuilder<Void, CloudException>(this.client.mapperAdapter())
                .register(204, new TypeToken<Void>() { }.getType())
                .build(response);
    }

}