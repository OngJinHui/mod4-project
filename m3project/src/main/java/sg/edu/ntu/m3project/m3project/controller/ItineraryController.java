package sg.edu.ntu.m3project.m3project.controller;

import org.apache.catalina.connector.Response;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import sg.edu.ntu.m3project.m3project.entity.Accommodation;
import sg.edu.ntu.m3project.m3project.entity.Destination;
import sg.edu.ntu.m3project.m3project.entity.Itinerary;
import sg.edu.ntu.m3project.m3project.entity.ItineraryItem;
import sg.edu.ntu.m3project.m3project.entity.Transport;

import sg.edu.ntu.m3project.m3project.entity.User;
import sg.edu.ntu.m3project.m3project.repo.AccommodationRepository;
import sg.edu.ntu.m3project.m3project.repo.DestinationRepository;
import sg.edu.ntu.m3project.m3project.repo.ItineraryItemRepository;
import sg.edu.ntu.m3project.m3project.repo.ItineraryRepository;
import sg.edu.ntu.m3project.m3project.repo.TransportRepository;
import sg.edu.ntu.m3project.m3project.repo.UserRepository;
import sg.edu.ntu.m3project.m3project.service.ItineraryService;
import sg.edu.ntu.m3project.m3project.service.ValidationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;

import java.net.URI;
import java.sql.Date;
import java.util.Arrays;
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
    TransportRepository transportRepo;

    @Autowired
    AccommodationRepository accommodationRepo;

    @Autowired
    UserRepository userRepo;

    @Autowired
    ItineraryService itineraryService;

    @Autowired
    ValidationService validationService;

    @GetMapping
    public ResponseEntity<List<Itinerary>> getAllItineraries() {
        List<Itinerary> itineraryRecords = (List<Itinerary>) itineraryRepo.findAll();
        return ResponseEntity.ok().body(itineraryRecords);
    }

    @GetMapping(value = "/{itineraryId}")
    public ResponseEntity<Itinerary> getItinerary(@PathVariable int itineraryId) {
        Itinerary itinerary = itineraryRepo.findById(itineraryId).orElse(null);
        if (itinerary == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().body(itinerary);
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
        Itinerary itinerary = itineraryRepo.findById(itineraryId).orElse(null);
        if (itinerary == null) {
            return ResponseEntity.badRequest().build();
        }
        List<ItineraryItem> itineraryItems = itineraryItemRepo.findAllByItinerary(itinerary);
        return ResponseEntity.ok().body(itineraryItems);
    }

    @PostMapping
    public ResponseEntity<Itinerary> createItinerary(@RequestBody Itinerary itinerary) {
        try {
            Itinerary createdItinerary = itineraryRepo.save(itinerary);
            return itineraryService.createdResponse(createdItinerary, createdItinerary.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/{itineraryId}")
    public ResponseEntity addItineraryItem(@PathVariable int itineraryId, @RequestBody ItineraryItem itineraryItem) {
        Itinerary itinerary = itineraryRepo.findById(itineraryId).orElse(null);
        if (itinerary == null) {
            return ResponseEntity.badRequest().build();
        }

        itineraryItem.setItinerary(itinerary);
        itineraryItemRepo.save(itineraryItem);

        return itineraryService.createdResponse(itineraryItem, itineraryItem.getId());
    }

    @PutMapping(value = "/{itineraryId}")
    public ResponseEntity updateItinerary(@PathVariable int itineraryId, @RequestBody Itinerary updatedItinerary) {
        Itinerary existingItinerary = itineraryRepo.findById(itineraryId).orElse(null);
        if (existingItinerary == null) {
            return ResponseEntity.badRequest().build();
        }
        existingItinerary.setName(updatedItinerary.getName());
        existingItinerary.setDescription(updatedItinerary.getDescription());
        existingItinerary.setStartDate(updatedItinerary.getStartDate());
        existingItinerary.setEndDate(updatedItinerary.getEndDate());
        existingItinerary.setBudget(updatedItinerary.getBudget());
        itineraryRepo.save(existingItinerary);
        return ResponseEntity.ok().build();
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

    @DeleteMapping(value = "/{itineraryId}")
    public ResponseEntity deleteItinerary(@PathVariable int itineraryId) {
        Optional<Itinerary> foundItinerary = itineraryRepo.findById(itineraryId);
        if (!foundItinerary.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        itineraryRepo.delete(foundItinerary.get());
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/{itineraryId}/budget")
    public ResponseEntity setBudget(@PathVariable int itineraryId,
            @RequestParam float budget) {

        validationService.validateBudget(budget);

        Optional<Itinerary> itineraryOptional = itineraryRepo.findById(itineraryId);
        if (!itineraryOptional.isPresent()) {
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

    @PutMapping(value = "/{itineraryId}/dates")
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

    @GetMapping(value = "/{itineraryId}/balance")
    public ResponseEntity<Double> getBudgetBalance(@PathVariable int itineraryId) {
        double balance = itineraryService.getBudgetBalance(itineraryId);
        if (balance >= 0) {
            return ResponseEntity.ok(balance);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/countries")
    public List<Object> getCountries() {
        String url = "https://restcountries.com/v3.1/all?fields=name";
        RestTemplate restTemplate = new RestTemplate();
        try{
            Object[] countries = restTemplate.getForObject(url, Object[].class);
            return Arrays.asList(countries);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
