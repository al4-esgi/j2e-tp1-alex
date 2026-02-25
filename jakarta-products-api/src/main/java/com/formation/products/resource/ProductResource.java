package com.formation.products.resource;

import java.net.URI;
import java.util.List;

import com.formation.products.model.Product;
import com.formation.products.service.ProductService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * ProductResource - Couche Presentation (REST API)
 * Expose les endpoints REST pour la gestion des produits
 * Délègue la logique métier au ProductService
 *
 * @Path : Définit le chemin de base de la ressource
 */
@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @Inject
    private ProductService productService;

    @Context
    private UriInfo uriInfo;

    /**
     * GET /products - Liste tous les produits ou filtre par catégorie
     * @param category paramètre optionnel pour filtrer par catégorie
     * @return Response avec la liste des produits (200 OK)
     */
    @GET
    public Response getAllProducts(@QueryParam("category") String category) {
        List<Product> products;

        if (category != null && !category.trim().isEmpty()) {
            products = productService.getProductsByCategory(category);
        } else {
            products = productService.getAllProducts();
        }

        return Response.ok(products).build();
    }

    /**
     * GET /products/{id} - Récupère un produit par son ID
     * @param id l'identifiant du produit
     * @return Response avec le produit (200 OK) ou erreur (404 NOT FOUND)
     */
    @GET
    @Path("/{id}")
    public Response getProduct(@PathParam("id") String id) {
        return productService.getProduct(id)
                .map(product -> Response.ok(product).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorMessage("Produit non trouvé avec l'ID: " + id))
                        .build());
    }

    /**
     * POST /products - Crée un nouveau produit
     * @param product le produit à créer
     * @return Response avec le produit créé (201 CREATED) et header Location
     */
    @POST
    public Response createProduct(Product product) {
        try {
            Product created = productService.createProduct(product);

            // Construire l'URI du produit créé
            URI location = uriInfo.getAbsolutePathBuilder()
                    .path(created.getId())
                    .build();

            return Response.created(location)
                    .entity(created)
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage(e.getMessage()))
                    .build();
        }
    }

    /**
     * PUT /products/{id} - Met à jour un produit existant
     * @param id l'identifiant du produit
     * @param product les nouvelles données
     * @return Response avec le produit mis à jour (200 OK) ou erreur
     */
    @PUT
    @Path("/{id}")
    public Response updateProduct(@PathParam("id") String id, Product product) {
        try {
            Product updated = productService.updateProduct(id, product);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage(e.getMessage()))
                    .build();
        }
    }

    /**
     * DELETE /products/{id} - Supprime un produit
     * @param id l'identifiant du produit à supprimer
     * @return Response vide (204 NO CONTENT) ou erreur (404 NOT FOUND)
     */
    @DELETE
    @Path("/{id}")
    public Response deleteProduct(@PathParam("id") String id) {
        try {
            productService.deleteProduct(id);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage(e.getMessage()))
                    .build();
        }
    }

    /**
     * PATCH /products/{id}/stock - Ajuste le stock d'un produit
     * @param id l'identifiant du produit
     * @param stockUpdate objet contenant la quantité à ajouter
     * @return Response avec le produit mis à jour (200 OK) ou erreur
     */
    @PATCH
    @Path("/{id}/stock")
    public Response updateStock(@PathParam("id") String id, StockUpdate stockUpdate) {
        try {
            Product updated = productService.updateStock(id, stockUpdate.getQuantity());
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage(e.getMessage()))
                    .build();
        }
    }

    /**
     * GET /products/count - Compte le nombre total de produits
     * @return Response avec le count (200 OK)
     */
    @GET
    @Path("/count")
    public Response countProducts() {
        long count = productService.countProducts();
        return Response.ok(new CountResponse(count)).build();
    }

    /**
     * Classe interne pour les messages d'erreur
     */
    public static class ErrorMessage {
        private String message;

        public ErrorMessage() {}

        public ErrorMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Classe interne pour les mises à jour de stock
     */
    public static class StockUpdate {
        private int quantity;

        public StockUpdate() {}

        public StockUpdate(int quantity) {
            this.quantity = quantity;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    /**
     * Classe interne pour les réponses de comptage
     */
    public static class CountResponse {
        private long count;

        public CountResponse() {}

        public CountResponse(long count) {
            this.count = count;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}
