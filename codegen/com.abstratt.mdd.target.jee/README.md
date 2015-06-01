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

### JAX-RS Resources

E4J generates JAX-RS resources backed by JPA services. It produces/consumes JSON representations compatible with the [Kirra API](http://github.com/abstratt/kirra) (so it can get a free dynamic UI etc).

#### single resource GET/PUT

Relationships TBD

#### list resource POST

Relationships TBD

#### list resource GET

Relationships TBD

### Maven support

Done.
