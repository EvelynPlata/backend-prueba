package com.lectura.backend.resource;

import com.lectura.backend.model.OrderDto;
import com.lectura.backend.service.IWooCommerceService;
import org.eclipse.microprofile.faulttolerance.Fallback;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.NotSupportedException;
import javax.transaction.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Objects;

@RolesAllowed("Admin")
@Path("/lectura/api/woocommerce")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WoocommerceResource {
    @Inject
    IWooCommerceService wooCommerceService;

    @Fallback(fallbackMethod = "fallbackResponse")
    @POST
    public Response post() throws Exception {
        var result = wooCommerceService.synchronization();
        if (result) {
            return Response.ok("The operation is processing another task ...").build();
        }
        return Response.ok("Processed").build();
    }

    @GET
    @Path("/simulate-sale/{productId}")
    public Response simulateSale(@PathParam("productId") Long productId, @QueryParam("price") Double price,
                                 @QueryParam("sku") String sku) throws Exception {
        var response = wooCommerceService.simulateSale(productId, price);
        return Response.ok(response).build();
    }

    @POST
    @Path("/sale/{productId}")
    public Response registerSale(@PathParam("productId") Long productId, OrderDto order) throws Exception {
        if (!productId.equals(order.getProductId())) {
            throw new BadRequestException("ProductID should be the same in the body");
        }
        return Response.status(Response.Status.CREATED)
                .entity(wooCommerceService.registerSale(order)).build();
    }

    @GET
    @Path("/sale/{orderId}/status")
    public Response getSaleStatus(@PathParam("orderId") String orderId) throws Exception {
        var sale = wooCommerceService.getSaleStatus(orderId);
        if (Objects.isNull(sale)) {
            throw new NotFoundException("Sale not found");
        }

        return Response.ok(sale).build();
    }

    @GET
    @Path("/sale/{orderId}/regenerate-token")
    public Response registerSale(@PathParam("orderId") String orderId) throws Exception {
        var newToken = wooCommerceService.regenerateSaleToken(orderId);
        if (Objects.isNull(newToken)) {
            throw new NotFoundException("Couldn't regenerate a new token for this sale");
        }
        return Response.ok(newToken).build();
    }

    @PermitAll
    @GET
    @Path("/download/{token}")
    public Response getDownloadUrl(@PathParam("token") String token, @QueryParam("uname") String uname) throws Exception {
        var downloadUrl = wooCommerceService.getDownloadUrl(token, uname);
        if (Objects.isNull(downloadUrl)) {
            return Response.noContent().build();
        }
        return Response.temporaryRedirect(downloadUrl).build();
    }

    public Response fallbackResponse() {
        return Response.accepted().entity("The operation is processing in background ...").build();
    }
}
