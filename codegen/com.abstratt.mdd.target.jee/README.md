Cloudfier Expert for Java (E4J)
==============

A code generator that can produce fully functional Java EE applications from Cloudfier models. 

Examples: 

https://textuml.ci.cloudbees.com/job/codegen-examples-JEE/ws/jee/

How to use:

https://github.com/abstratt/cloudfier-maven-plugin (via Maven)

https://github.com/abstratt/codegen-examples (from bash scripts)

## Features

The sections below describe the various features of the JavaEE generator, in the following format:

Modeled:

\<example-model\>

Generated:

\<example-generated-code\>



### JPA entities and services 

#### Attribute invariants

Modeled:
```
class Car

    /* ...*/
    
    attribute price : Double := 500.0
        (* Price mustbe $50 at least. *)
        invariant PriceAboveMinimum { self.price >= 50.0 }
        (* Price cannot be above $500. *)
        invariant PriceBelowMaximum { self.price <= 500.0 };
```

Generated:
```
@Entity
public class Car {
    /* ... */
    
    @Column(nullable=false)
    private double price = 500.0;
    
    public double getPrice() {
        return this.price;
    }
    
    public void setPrice(double newPrice) {
        if (!(newPrice >= 50.0)) {
            throw new PriceAboveMinimumException();
        }
        if (!(newPrice <= 500.0)) {
            throw new PriceBelowMaximumException();
        }
        this.price = newPrice;
    }
```

#### Preconditions

Modeled:
```
    protected operation finish()
        precondition MustBeInProgress { self.inProgress };
    begin
        /* ... */
    end;
```

Generated:
```
    protected void finish() {
        if (!this.isInProgress()) {
            throw new MustBeInProgressException();
        }
        /* ... */
    }
```
#### Actions

Modeled:
```
    operation rent(car : Car)
        precondition CarMustBeAvailable(car) { car.available }
        precondition CustomerMustHaveNoCurrentRental { self.currentRental == null };
    begin
        var rental;
        rental := new Rental;
        link RentalsCustomer(customer := self, rentals := rental);
        link RentalsCar(car := car, rentals := rental);
        send CarRented() to car;
    end;
```

Generated:
```
    public void rent(Car car) {
        if (!car.isAvailable()) {
            throw new CarMustBeAvailableException();
        }
        if (!((this.getCurrentRental() == null))) {
            throw new CustomerMustHaveNoCurrentRentalException();
        }
        Rental rental;
        rental = new Rental();
        rental.setCustomer(this);
        this.addToRentals(rental);
        rental.setCar(car);
        car.addToRentals(rental);
        car.handleEvent(Car.StatusEvent.CarRented);
        persist(rental);
    }
```
#### State machines

Modeled:

```
class Car
    /* ... */
    
    attribute status : Status;
    statemachine Status

        initial state Available
            transition on signal(CarRented) to Rented;
            transition on signal(RepairStarted) to UnderRepair;
        end;

        state Rented
            transition on signal(CarReturned) to Available;
        end;

        state UnderRepair
            transition on signal(RepairFinished) to Available;
        end;

    end;
```

Generated:
```
public class Car {
    /* ... */
    
    @Column(nullable=false)
    @Enumerated(EnumType.STRING)
    private Car.Status status = Car.Status.Available;
    
    /* ... */
    
    /*************************** STATE MACHINE ********************/
    
    public enum Status {
        Available {
            @Override void handleEvent(Car instance, StatusEvent event) {
                switch (event) {
                    case CarRented :
                        doTransitionTo(instance, Rented);
                        break;
                    
                    case RepairStarted :
                        doTransitionTo(instance, UnderRepair);
                        break;
                    default : break; // unexpected events are silently ignored 
                }
            }                       
        },
        Rented {
            @Override void handleEvent(Car instance, StatusEvent event) {
                switch (event) {
                    case CarReturned :
                        doTransitionTo(instance, Available);
                        break;
                    default : break; // unexpected events are silently ignored 
                }
            }                       
        },
        UnderRepair {
            @Override void handleEvent(Car instance, StatusEvent event) {
                switch (event) {
                    case RepairFinished :
                        doTransitionTo(instance, Available);
                        break;
                    default : break; // unexpected events are silently ignored 
                }
            }                       
        };
        void onEntry(Car instance) {
            // no entry behavior by default
        }
        void onExit(Car instance) {
            // no exit behavior by default
        }
        /** Each state implements handling of events. */
        abstract void handleEvent(Car instance, StatusEvent event);
        /** 
            Performs a transition.
            @param instance the instance to update
            @param newState the new state to transition to 
        */
        final void doTransitionTo(Car instance, Status newState) {
            instance.status.onExit(instance);
            instance.status = newState;
            instance.status.onEntry(instance);
        }
    }
    
    public enum StatusEvent {
        CarRented,
        RepairStarted,
        CarReturned,
        RepairFinished
    }
    
    public void handleEvent(StatusEvent event) {
        status.handleEvent(this, event);
    }
}
```
#### Queries

Modeled:
```
class Rental
    /* ... */
    static query inProgress() : Rental[*];
    begin
        return Rental extent.select((l : Rental) : Boolean {
            l.inProgress
        });
    end;
end;
```

Generated:

```
public class RentalService {
    /* ... */
    
    public Collection<Rental>  inProgress() {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Rental> cq = cb.createQuery(Rental.class);
        Root<Rental> rental_ = cq.from(Rental.class);
        return getEntityManager().createQuery(
            cq.distinct(true).where(
                cb.equal(rental_.get("returned"), cb.nullLiteral(null))
            )
        ).getResultList();
    }
}
```

#### Aggregation queries

TBD

### JAX-RS based REST API

E4J generates JAX-RS resources backed by JPA services. It produces/consumes JSON representations compatible with the [Kirra API](http://github.com/abstratt/kirra) (so it can get a free dynamic UI etc).

#### JAX-RS resource

```
package resource.car_rental;

import car_rental.*;

import java.util.*;
import java.util.stream.*;
import java.text.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;        
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import java.net.URI;

@Path("entities/car_rental.Car/instances")
@Produces(MediaType.APPLICATION_JSON)
public class CarResource {
    
        private static ResponseBuilder status(Status status) {
            return Response.status(status).type(MediaType.APPLICATION_JSON);
        }
    
        @Context
        UriInfo uri;
    
        private CarService service = new CarService();
```

#### single resource GET

```
        @GET
        @Path("{id}")
        public Response getSingle(@PathParam("id") String idString) {
            if ("_template".equals(idString)) {
                Car template = new Car(); 
                return status(Status.OK).entity(toExternalRepresentation(template, uri.getRequestUri().resolve(""), true)).build();
            }
            Long id = Long.parseLong(idString);
            Car found = service.find(id);
            if (found == null)
                return status(Status.NOT_FOUND).entity(Collections.singletonMap("message", "Car not found: " + id)).build();
            return status(Status.OK).entity(toExternalRepresentation(found, uri.getRequestUri().resolve(""), true)).build();
        }
```

#### single resource PUT

```
        @PUT
        @Path("{id}")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response put(@PathParam("id") Long id, Map<String, Object> representation) {
            Car found = service.find(id);
            if (found == null)
                return status(Status.NOT_FOUND).entity("Car not found: " + id).build();
            try {    
                updateFromExternalRepresentation(found, representation);
            } catch (RuntimeException e) {
                return status(Status.BAD_REQUEST).entity(Collections.singletonMap("message", e.getMessage())).build();
            }    
            service.update(found);
            return status(Status.OK).entity(toExternalRepresentation(found, uri.getRequestUri().resolve(""), true)).build();
        }
```

#### list resource POST

```
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        public Response post(Map<String, Object> representation) {
            Car newInstance = new Car();
            try {    
                updateFromExternalRepresentation(newInstance, representation);
            } catch (RuntimeException e) {
                return status(Status.BAD_REQUEST).entity(Collections.singletonMap("message", e.getMessage())).build();
            }    
            service.create(newInstance);
            return status(Status.CREATED).entity(toExternalRepresentation(newInstance, uri.getRequestUri().resolve(newInstance.getId().toString()), true)).build();
        }

```

#### list resource GET

```
        @GET
        public Response getList() {
            Collection<Car> models = service.findAll();
            URI extentURI = uri.getRequestUri();
            Collection<Map<String, Object>> items = models.stream().map(toMap -> {
                return toExternalRepresentation(toMap, extentURI, true);
            }).collect(Collectors.toList());
            
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("contents", items);
            result.put("offset", 0);
            result.put("length", items.size());  
            return status(Status.OK).entity(result).build();
        }
```
#### Converting domain instances from/to JSON

(relationship support TBD)

Modeled:
```
class Car

    derived attribute description : String /* ...*/;
    attribute plate : String;
    attribute price : Double  /* ...*/;
    derived attribute available : Boolean  /* ...*/;
    derived attribute currentRental : Rental /* ...*/;
    attribute year : Integer /* ...*/;
    attribute color : String[0,1];
    readonly attribute carModel : CarModel;
    derived attribute underRepair : Boolean /* ...*/;
    derived attribute rented : Boolean /* ...*/;
    readonly attribute rentals : Rental[*];
    attribute status : Status;
    /* ... */
```

Generated:
```
        private Map<String, Object> toExternalRepresentation(Car toRender, URI instancesURI, boolean full) {
            Map<String, Object> result = new LinkedHashMap<>();
            Map<String, Object> values = new LinkedHashMap<>();
            boolean persisted = toRender.getId() != null;
            values.put("plate", toRender.getPlate());
            values.put("price", toRender.getPrice());
            values.put("year", toRender.getYear());
            values.put("color", toRender.getColor());
            values.put("status", toRender.getStatus().name());
            if (persisted) {
                values.put("description", toRender.getDescription());
                values.put("available", toRender.isAvailable());
                values.put("underRepair", toRender.isUnderRepair());
                values.put("rented", toRender.isRented());
            } else {
                values.put("description", "");
                values.put("available", false);
                values.put("underRepair", false);
                values.put("rented", false);
            }
            result.put("values", values);
            Map<String, Object> links = new LinkedHashMap<>();
            result.put("links", links);
            result.put("uri", instancesURI.resolve(persisted ? toRender.getId().toString() : "_template").toString());
            result.put("entityUri", instancesURI.resolve("../..").resolve("car_rental.Car").toString());
            if (persisted) {
                result.put("objectId", toRender.getId().toString());
                result.put("shorthand", toRender.getDescription());
            }
            result.put("full", full);
            result.put("disabledActions", Collections.emptyMap());
            result.put("scopeName", "Car");
            result.put("scopeNamespace", "car_rental");
            Map<String, Object> typeRef = new LinkedHashMap<>();
            typeRef.put("entityNamespace", "car_rental");
            typeRef.put("kind", "Entity");
            typeRef.put("typeName", "Car");
            typeRef.put("fullName", "car_rental.Car");
            result.put("typeRef", typeRef);   
            return result;                    
        }
        
        private void updateFromExternalRepresentation(Car toUpdate, Map<String, Object> external) {
            Map<String, Object> values = (Map<String, Object>) external.get("values");
            if (values.get("plate") != null)
                toUpdate.setPlate((String) values.get("plate"));
            else
                toUpdate.setPlate("");
            if (values.get("price") != null)
                toUpdate.setPrice(Double.parseDouble((String) values.get("price")));
            else
                toUpdate.setPrice(500.0);
            if (values.get("year") != null)
                toUpdate.setYear(Long.parseLong((String) values.get("year")));
            else
                toUpdate.setYear(java.sql.Date.valueOf(java.time.LocalDate.now()).getYear() + 1900L);
            toUpdate.setColor((String) values.get("color"));
        }    
```

### Maven support

#### mvn test

Includes support for running the tests generated (JPA CRUD and functional).

#### mvn install

Produces a deployable WAR file containing the application, which comprises a persistent domain layer using JPA, exposed via a REST API using JAX-RS.

#### mvn exec:java

Launches the application as a standalone server (using Jetty). You can then use Kirra Qooxdoo to access the application via a mobile-styled web UI using [Kirra Qooxdoo](https://github.com/abstratt/kirra/tree/master/kirra_qooxdoo).
