package com.scheduling.serviceImplimentation;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.scheduling.dto.BookingDto;
import com.scheduling.dto.BookingRequestDto;
import com.scheduling.dto.ExpertResponseDto;
import com.scheduling.dto.OrderRequestDto;
import com.scheduling.dto.OrderResponse;
import com.scheduling.dto.SlotDetailsDtoUser;
import com.scheduling.model.SlotAvailable;
import com.scheduling.model.SlotBooking;
import com.scheduling.model.UserEntity;
import com.scheduling.repository.SlotBookingRepository;
import com.scheduling.services.EmailService;
import com.scheduling.services.SlotBookingService;
import com.scheduling.services.SlotServices;
import com.scheduling.services.UserServices;

@Service
public class BookingServiceImplimentation implements SlotBookingService {

    @Autowired
    private SlotServices slotServices;
    
    @Autowired 
    private UserServices userServices;
    
    @Autowired
    private SlotBookingRepository slotBookingRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Override
    public OrderResponse addBooking(BookingRequestDto slotId, String username) {
        try {
            UserEntity userEntity =  userServices.getUserByEmail(username);
            SlotAvailable slotAvailable  =  slotServices.getslotByID(slotId.getSlotId());
            if (slotAvailable!=null) {
                SlotBooking slotBooking = new SlotBooking();
                slotBooking.setBookingDate(Date.valueOf(LocalDate.now()));
                slotBooking.setBookingTime(Time.valueOf(LocalTime.now()));
                slotBooking.setBookingStatus("pending");
                slotBooking.setExpert(slotAvailable.getExpert());
                slotBooking.setUserEntity(userEntity);
                slotBooking.setSlotAvailable(slotAvailable);
                RazorpayClient razorpayClient = new RazorpayClient("rzp_test_9B8AXwdinhrVTx", "aeRfMEbWIgA3mA2fOsY7LU0G");
                JSONObject options = new JSONObject();
                options.put("amount", 25000);
                options.put("currency", "INR");
                Order order = razorpayClient.Orders.create(options);
                slotBooking.setOrederId(order.get("id"));
                System.out.println(order.get("id").toString());
                slotBooking.setPaymentId("");
                slotBooking.setAmount("250");
                slotBooking.setUserEmail(slotId.getEmailId());
                slotBooking.setUserMobile(slotId.getPhoneNumber());
                slotBooking.setCategory(slotId.getCategory());
                slotBookingRepository.save(slotBooking);
                OrderResponse orderResponse = new OrderResponse();
                orderResponse.setOrderId(order.get("id"));
                orderResponse.setStatus("created");
                return orderResponse;
            }else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<BookingDto> getUserBookedSlotByEmail(String username) {
        
        try {
            UserEntity userEntity = userServices.getUserByEmail(username);
            
            List<SlotBooking> slotBookings = slotBookingRepository.findByUserEntitys(userEntity);
            
            List<BookingDto> bookingDtos = new ArrayList<>();
            
            slotBookings.stream().forEach(slotBooking -> {
                bookingDtos.add(converter(slotBooking));
            });
            
            if (slotBookings!=null) {
                return bookingDtos;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    private BookingDto converter(SlotBooking slotBooking) {
        BookingDto bookingDto = new BookingDto();
        bookingDto.setBookingId(slotBooking.getBookingId());
        bookingDto.setBookingDate(slotBooking.getBookingDate());
        bookingDto.setBookingStatus(slotBooking.getBookingStatus());
        bookingDto.setBookingTime(slotBooking.getBookingTime());
        bookingDto.setExpertDto(new ExpertResponseDto(slotBooking.getExpert().getExpertId(),slotBooking.getExpert().getExpertName(), slotBooking.getExpert().getExpertEmail(), slotBooking.getExpert().getExpertType(), slotBooking.getExpert().getConversationCount(), slotBooking.getExpert().getRating(),slotBooking.getExpert().getExpertProfile()));
        bookingDto.setSlotDetailsDtoUser(new SlotDetailsDtoUser(slotBooking.getSlotAvailable().getSlotId(), slotBooking.getSlotAvailable().getSloDate(), slotBooking.getSlotAvailable().getStars(), slotBooking.getSlotAvailable().getEnds()));
        bookingDto.setOrederId(slotBooking.getOrederId());
        bookingDto.setAmount(250+"");
        bookingDto.setPaymentId(slotBooking.getPaymentId());
        return bookingDto;
    }

    @Override
    public BookingDto updateBooking(OrderRequestDto order,String username) {
        try {
            SlotBooking slotBooking = slotBookingRepository.getByOrderId(order.getOrderId());
            RazorpayClient razorpayClient = new RazorpayClient("rzp_test_9B8AXwdinhrVTx", "aeRfMEbWIgA3mA2fOsY7LU0G");
            Payment payment = razorpayClient.Payments.fetch(order.getPaymentId());
            if (payment!=null && payment.get("amount").toString().trim().equals("25000") && payment.get("order_id").toString().trim().equals(order.getOrderId()) && payment.get("status").toString().trim().equals("captured")) {
                if(slotBooking!=null && !slotBooking.getBookingStatus().equals("paid")) {
                    if (slotServices.bookslot(slotBooking.getSlotAvailable())) {
                        slotBooking.setSlotAvailable(slotBooking.getSlotAvailable());
                    }
                    slotBooking.setBookingDate(Date.valueOf(LocalDate.now()));
                    slotBooking.setBookingTime(Time.valueOf(LocalTime.now()));
                    slotBooking.setBookingStatus("paid");
                    slotBooking.setPaymentId(order.getPaymentId());
                    SlotBooking save = slotBookingRepository.save(slotBooking);
                     BookingDto converter = converter(save);
                    if (save.getUserEmail()!=null) {
                        if (save.getUserEmail().equals(username.trim())) {
                            emailService.sendBookingMail(save.getUserEmail(),converter);
                        }else {
                            emailService.sendBookingMail(username,converter);
                        }
                    }else {
                        emailService.sendBookingMail(username,converter);
                    }
                    return converter;
                } 
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
