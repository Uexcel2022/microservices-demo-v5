package com.eazybytes.accounts.service.impl;

import com.eazybytes.accounts.dto.*;
import com.eazybytes.accounts.entity.Account;
import com.eazybytes.accounts.entity.Customer;
import com.eazybytes.accounts.exception.InvalidArgException;
import com.eazybytes.accounts.exception.ResourceNotFoundException;
import com.eazybytes.accounts.mapper.AccountMapper;
import com.eazybytes.accounts.mapper.CustomerMapper;
import com.eazybytes.accounts.repository.AccountRepository;
import com.eazybytes.accounts.repository.CustomerRepository;
import com.eazybytes.accounts.service.ICustomerService;
import com.eazybytes.accounts.service.client.CardFeignClient;
import com.eazybytes.accounts.service.client.LoanFeignClient;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Transactional
@AllArgsConstructor
public class ICustomerServiceImpl implements ICustomerService {

    private static final Logger log = LoggerFactory.getLogger(ICustomerServiceImpl.class);
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private  final LoanFeignClient loanFeignClient;
    private  final CardFeignClient cardFeignClient;

    /**
     * @param mobileNumber - customer mobile mobile
     * @return - Returns customer details with customerDetailsDto
     */
    @Override
    public CustomerDetailsDto getCustomerDetails(String mobileNumber, String correlationId) {

        if(!validateMobileNumber(mobileNumber)){
            throw new InvalidArgException("Invalid mobile number");
        }

        Customer customer =
                customerRepository.findByMobileNumber(mobileNumber)
                        .orElseThrow(()-> new
                                ResourceNotFoundException("Customer","mobileNumber", mobileNumber));

        Account account =
                accountRepository.findByCustomerId(customer.getCustomerId())
                        .orElseThrow(()-> new
                                ResourceNotFoundException("Customer","customerId",
                                customer.getCustomerId().toString()));

        CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer,new CustomerDetailsDto());
        customerDetailsDto.setAccountDto(AccountMapper.mapToAccountDto(account));

        CardDto cardDto = cardFeignClient.fetchCard(correlationId, mobileNumber).getBody();
        customerDetailsDto.setCardDto(cardDto);
        LoanDto loanDto = loanFeignClient.getLoanDetails(correlationId ,mobileNumber).getBody();
        customerDetailsDto.setLoanDto(loanDto);
        log.debug("eazybank-correlation-id found: {}", correlationId);
        return customerDetailsDto;
    }
}
