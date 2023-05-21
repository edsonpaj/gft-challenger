package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.AmountTransferDTO;
import com.dws.challenge.exception.NotFoundFundsException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.EmailNotificationService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    void createAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        Account account = accountsService.getAccount("Id-123");
        assertThat(account.getAccountId()).isEqualTo("Id-123");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void createDuplicateAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccountNegativeBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void createAccountEmptyAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    void getAccount() throws Exception {
        String uniqueAccountId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
        this.accountsService.createAccount(account);
        this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
    }

    @Test
    void amountTransfer() throws Exception {
        //Test setup -
        String sourceAccountId = "Test-amountTransfer-ac1";
        Account sourceAccount = new Account(sourceAccountId, new BigDecimal("100.00"));
        this.accountsService.createAccount(sourceAccount);
        String destinationAccountId = "Test-amountTransfer-ac2-" + System.currentTimeMillis();
        Account destinationAccount = new Account(destinationAccountId, new BigDecimal("0.01"));
        this.accountsService.createAccount(destinationAccount);

        Mockito.doNothing().when(notificationService).notifyAboutTransfer(Mockito.any(Account.class), Mockito.any(String.class));

        //Service Call
        this.accountsService.amountTransfer(AmountTransferDTO.builder()
                .sourceAccountId(sourceAccountId)
                .destinationAccountId(destinationAccountId)
                .transferAmount(new BigDecimal("50.01"))
                .build());

        //Test asserts -
        Account accountSourceEndStatus = accountsService.getAccount(sourceAccountId);
        Account accountDestinationEndStatus = accountsService.getAccount(destinationAccountId);
        assertThat(accountSourceEndStatus.getBalance()).isEqualByComparingTo("49.99");
        assertThat(accountDestinationEndStatus.getBalance()).isEqualByComparingTo("50.02");
    }
    @Test
    void amountTransferWithoutFunds() throws Exception {
        //Test setup -
        String sourceAccountId = "Test-amountTransferWithoutFunds-ac1" + System.currentTimeMillis();
        Account sourceAccount = new Account(sourceAccountId, new BigDecimal("10.00"));
        this.accountsService.createAccount(sourceAccount);
        String destinationAccountId = "Test-amountTransferWithoutFunds-ac2" + System.currentTimeMillis();
        Account destinationAccount = new Account(destinationAccountId, new BigDecimal("0.00"));
        this.accountsService.createAccount(destinationAccount);

        Mockito.doNothing().when(notificationService).notifyAboutTransfer(Mockito.any(Account.class), Mockito.any(String.class));

        Assertions.assertThrows(NotFoundFundsException.class, () ->
        //Service Call
        this.accountsService.amountTransfer(AmountTransferDTO.builder()
                .sourceAccountId(sourceAccountId)
                .destinationAccountId(destinationAccountId)
                .transferAmount(new BigDecimal("50.00"))
                .build())
        );
    }
}
