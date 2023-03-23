package sg.edu.ntu.m3project.m3project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import sg.edu.ntu.m3project.m3project.entity.Destination;
import sg.edu.ntu.m3project.m3project.entity.Itinerary;
import sg.edu.ntu.m3project.m3project.entity.ItineraryItem;
import sg.edu.ntu.m3project.m3project.entity.Transport;

import sg.edu.ntu.m3project.m3project.entity.User;
import sg.edu.ntu.m3project.m3project.repo.DestinationRepository;
import sg.edu.ntu.m3project.m3project.repo.ItineraryItemRepository;
import sg.edu.ntu.m3project.m3project.repo.ItineraryRepository;
import sg.edu.ntu.m3project.m3project.repo.TransportRepository;
import sg.edu.ntu.m3project.m3project.repo.UserRepository;
import sg.edu.ntu.m3project.m3project.service.ValidationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;

import java.net.URI;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/itineraries")
public class ItineraryController {
    @Autowired
    ItineraryRepository itineraryRepo;

    @Autowired
    ItineraryItemRepository itineraryItemRepo;

    @Autowired
    DestinationRepository destinationRepo;

    @Autowired
    UserRepository userRepo;

    @Autowired
    ValidationService validationService;

    @Autowired
    TransportRepository transportRepo;

    @GetMapping
    public ResponseEntity<List<Itinerary>> getAllItineraries() {
        List<Itinerary> itineraryRecords = (List<Itinerary>) itineraryRepo.findAll();
        return ResponseEntity.ok().body(itineraryRecords);
    }

    @GetMapping(value = "/users/{userId}")
    public ResponseEntity<List<Itinerary>> getUserItinerary(@PathVariable int userId) {
        List<Itinerary> userItinerary = (List<Itinerary>) itineraryRepo.findAllByUserId(userId);
        if (userItinerary.size() > 0) {
            return ResponseEntity.ok().body(userItinerary);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/{itineraryId}/items")
    public ResponseEntity<List<ItineraryItem>> getItineraryItems(@PathVariable int itineraryId) {
        Optional<Itinerary> itinerary = itineraryRepo.findById(itineraryId);
        if (!itinerary.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        List<ItineraryItem> itineraryItems = itineraryItemRepo.findAllByItinerary(itinerary.get());
        return ResponseEntity.ok().body(itineraryItems);
    }


    @PostMapping
    public ResponseEntity<Itinerary> createItinerary(@RequestBody Itinerary itinerary) {
        try {
            Itinerary createdItinerary = itineraryRepo.save(itinerary);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                    .buildAndExpand(createdItinerary.getId()).toUri();
            return ResponseEntity.created(location).body(createdItinerary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/{itineraryId}")
    public ResponseEntity addItineraryItem(@PathVariable int itineraryId, @RequestBody ItineraryItem itineraryItem) {

        Optional<Itinerary> itinerary = itineraryRepo.findById(itineraryId);

        if (!itinerary.isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        itineraryItem.setItinerary(itinerary.get());
        itineraryItemRepo.save(itineraryItem);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(itineraryItem.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/items/{itineraryItemId}/destination")
    public ResponseEntity setDestination(@PathVariable int itineraryItemId, @RequestParam int destinationId) {

        ItineraryItem itineraryItem = itineraryItemRepo.findById(itineraryItemId).orElse(null);    
        Destination destination = destinationRepo.findById(destinationId).orElse(null);

        if (itineraryItem == null || destination == null) {
            return ResponseEntity.badRequest().build();
        }
        // Validate destination country
        try {
            validationService.validateCountry(destination.getCountry());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        // validate city service
        try {
            validationService.validateCity(destination.getCity());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        

        itineraryItem.setDestination(destination);
        itineraryItemRepo.save(itineraryItem);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(itineraryItem.getId())
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/{userId}/destinations")
    public ResponseEntity deleteAllDestinations(@PathVariable int userId) {
        List<Itinerary> userItineraryList = (List<Itinerary>) itineraryRepo.findAllByUserId(userId);
        if (userItineraryList.size() == 0) {
            return ResponseEntity.notFound().build();
        }
        for (Itinerary itinerary : userItineraryList) {
            List<ItineraryItem> itineraryItemList = itineraryItemRepo.findAllByItineraryId(itinerary.getId());
            if (itineraryItemList.size() == 0) {
                return ResponseEntity.notFound().build();
            }
            for (ItineraryItem itineraryItem : itineraryItemList) {
                itineraryItem.setDestination(null);
                itineraryItemRepo.save(itineraryItem);
            }
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/{itineraryId}/destination")
    public ResponseEntity deleteDestination(@PathVariable int itineraryId) {
        Optional<ItineraryItem> itineraryItemOptional = itineraryItemRepo.findByitineraryId(itineraryId);
        if (!itineraryItemOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        ItineraryItem itineraryItemToUpdate = itineraryItemOptional.get();
        itineraryItemToUpdate.setDestination(null);
        itineraryItemRepo.save(itineraryItemToUpdate);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/items/{itineraryItemId}/accommodation")
    public ResponseEntity addAccommodation() {
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/transport")
    public ResponseEntity<Transport> create(@RequestBody Transport transport) {

        try {
            Transport created = transportRepo.save(transport); // when "id" is not present, .save() will perform create
                                                               // operation.
            return new ResponseEntity(transportRepo.findById(created.getId()), HttpStatus.CREATED);
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping(value = "items/{itineraryId}/{transportId}")
    public ResponseEntity<Transport> addTransport(@RequestBody Transport transport, @PathVariable Integer userId,
            @PathVariable Integer itineraryId, @PathVariable Integer transportId) {
        Optional<Transport> currentTransport = transportRepo.findById(transportId);
        if (currentTransport.isPresent()) { // Check if the expected object is present
            try {
                Transport t = currentTransport.get(); // Get the object - Transport

                // Update the fetched product with description, price sent via Request Body
                t.setDescription(transport.getDescription());
                t.setPrice(transport.getPrice());

                transportRepo.save(t); // When "id" is present, .save() will perform update operation.
                return ResponseEntity.ok().body(t);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping(value = "/{userId}/{itineraryId}/accommodation")
    public ResponseEntity deleteAccommodation() {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{userId}/{itineraryId}/{transportId}")
    public ResponseEntity deleteTransport(@PathVariable int userId, @PathVariable int itineraryId,
            @PathVariable int transportId) {
        boolean exist = transportRepo.existsById(transportId);
        if (exist) {
            try {
                transportRepo.deleteById(transportId);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping(value="/{itineraryId}")
    public ResponseEntity deleteItinerary(@PathVariable int itineraryId) {
        Optional<Itinerary> foundItinerary = itineraryRepo.findById(itineraryId);
        if (!foundItinerary.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        itineraryRepo.deleteById(itineraryId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value="/items/{itineraryItemId}")
    public ResponseEntity deleteItineraryItem(@PathVariable int itineraryItemId) {
        Optional<ItineraryItem> foundItineraryItem = itineraryItemRepo.findById(itineraryItemId);
        if (!foundItineraryItem.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        itineraryItemRepo.deleteById(itineraryItemId);
        return ResponseEntity.ok().build();
    }

    // Endpoint eg: http://localhost:8080/itineraries/1/1/budget?budget=999
    @PutMapping(value = "/{userId}/{itineraryId}/budget")
    public ResponseEntity setBudget(@PathVariable int userId, @PathVariable int itineraryId,
            @RequestParam float budget) {

        if (ObjectUtils.isEmpty(budget) || budget < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget cannot be negative");
        } else if (budget > 99999999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget limit exceeded");
        }

        Optional<Itinerary> itineraryOptional = itineraryRepo.findById(itineraryId);
        Optional<User> userOptional = userRepo.findById(userId);
        if (!itineraryOptional.isPresent() || !userOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Itinerary itineraryBudgetToSet = itineraryOptional.get();
        try {
            itineraryBudgetToSet.setBudget(budget);
            itineraryRepo.save(itineraryBudgetToSet);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{itineraryId}/dates")
    public ResponseEntity<Itinerary> setItineraryDates(@PathVariable int itineraryId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        Optional<Itinerary> foundItinerary = itineraryRepo.findById(itineraryId);
        if (!foundItinerary.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Itinerary itineraryToUpdate = foundItinerary.get();
        itineraryToUpdate.setStartDate(startDate);
        itineraryToUpdate.setEndDate(endDate);
        itineraryRepo.save(itineraryToUpdate);
        return ResponseEntity.ok().body(itineraryToUpdate);
    }

    @PutMapping("/items/{itineraryItemId}/dates")
    public ResponseEntity<ItineraryItem> setItineraryItemDates(@PathVariable int itineraryItemId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        Optional<ItineraryItem> itineraryItemOptional = itineraryItemRepo.findById(itineraryItemId);
        if (!itineraryItemOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        ItineraryItem itineraryItemToUpdate = itineraryItemOptional.get();
        itineraryItemToUpdate.setStartDate(startDate);
        itineraryItemToUpdate.setEndDate(endDate);
        itineraryItemRepo.save(itineraryItemToUpdate);
        return ResponseEntity.ok().body(itineraryItemToUpdate);
    }

}
