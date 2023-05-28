package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.AmountTransferDTO;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        assertThat(account.getBalance().get()).isEqualByComparingTo("1000");
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
        String sourceAccountId = setupTestInsertAccount("Test-amountTransfer-ac1", "100.00");
        String destinationAccountId = setupTestInsertAccount("Test-amountTransfer-ac2-", "0.01");
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
        assertThat(accountSourceEndStatus.getBalance().get()).isEqualByComparingTo("49.99");
        assertThat(accountDestinationEndStatus.getBalance().get()).isEqualByComparingTo("50.02");
    }
    @Test
    void amountTransferWithoutFunds() throws Exception {
        //Test setup -
        String sourceAccountId = setupTestInsertAccount("Test-amountTransferWithoutFunds-ac1", "10.00");
        String destinationAccountId = setupTestInsertAccount("Test-amountTransferWithoutFunds-ac2", "0.00");
        Mockito.doNothing().when(notificationService).notifyAboutTransfer(Mockito.any(Account.class), Mockito.any(String.class));

        //Service Call
        this.accountsService.amountTransfer(AmountTransferDTO.builder()
                .sourceAccountId(sourceAccountId)
                .destinationAccountId(destinationAccountId)
                .transferAmount(new BigDecimal("50.00"))
                .build());

        //Test asserts -
        Account accountSourceEndStatus = accountsService.getAccount(sourceAccountId);
        Account accountDestinationEndStatus = accountsService.getAccount(destinationAccountId);
        assertThat(accountSourceEndStatus.getBalance().get()).isEqualByComparingTo("10.00");
        assertThat(accountDestinationEndStatus.getBalance().get()).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest
    @CsvSource({
            "ac1,10.00,10.00,ac2,0.00,0.00,50.00",
            "ac3,100.00,90.00,ac4,0.00,10.00,10.00",
            "ac5,20.00,15.00,ac6,5.00,10.00,5.00"
    })
    void multiplesTransfers(String acSourceId, String acSourceValue, String acSourceExpectedValue,
                            String acDestinationId, String acDestinationValue, String acDestinationExpectedValue,
                            String transferAmount) throws Exception {
        //Test setup -
        String ac1Id = setupTestInsertAccount(acSourceId, acSourceValue);
        String ac2Id = setupTestInsertAccount(acDestinationId, acDestinationValue);
        Mockito.doNothing().when(notificationService).notifyAboutTransfer(Mockito.any(Account.class), Mockito.any(String.class));

        //Service Call
        this.accountsService.amountTransfer(AmountTransferDTO.builder()
                .sourceAccountId(ac1Id)
                .destinationAccountId(ac2Id)
                .transferAmount(new BigDecimal(transferAmount))
                .build());

        //Test asserts -
        Account accountSourceEndStatus = accountsService.getAccount(ac1Id);
        Account accountDestinationEndStatus = accountsService.getAccount(ac2Id);
        assertThat(accountSourceEndStatus.getBalance().get()).isEqualByComparingTo(acSourceExpectedValue);
        assertThat(accountDestinationEndStatus.getBalance().get()).isEqualByComparingTo(acDestinationExpectedValue);
    }

    @Test
    void multiplesSimultaneousTransfersToSameAccount() throws Exception {
        //Test setup -
        List<String> listAccIds = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            listAccIds.add(setupTestInsertAccount("ac" + i, "100.00"));
        }
        Mockito.doNothing().when(notificationService).notifyAboutTransfer(Mockito.any(Account.class), Mockito.any(String.class));

        //Service Call
        Random random = new Random();
        String luckyGuy = listAccIds.get(random.nextInt(listAccIds.size()));

        listAccIds.parallelStream()
                .filter(adId -> !adId.equals(luckyGuy))
                .forEach(acId -> {
                    this.accountsService.amountTransfer(AmountTransferDTO.builder()
                            .sourceAccountId(acId)
                            .destinationAccountId(luckyGuy)
                            .transferAmount(new BigDecimal("10.00"))
                            .build());
                });

        //Test asserts -
        listAccIds.parallelStream()
                .filter(adId -> !adId.equals(luckyGuy))
                .forEach(ac -> {
                    Account accountStatus = accountsService.getAccount(ac);
                    assertThat(accountStatus.getBalance().get()).isEqualByComparingTo("90.00");
                });
        Account acLuckyGuy = accountsService.getAccount(luckyGuy);
        assertThat(acLuckyGuy.getBalance().get()).isEqualByComparingTo("1090.00");
    }

    @Test
    void multiplesSimultaneousTransfersFromSameAccount() throws Exception {
        //Test setup -
        List<String> listAccIds = new ArrayList<>();
        String acRichGuyId = setupTestInsertAccount("acRichGuy", "9000.00");
        for (int i = 1; i <= 100; i++) {
            listAccIds.add(setupTestInsertAccount("ac" + i, "10.00"));
        }
        Mockito.doNothing().when(notificationService).notifyAboutTransfer(Mockito.any(Account.class), Mockito.any(String.class));

        //Service Call
        listAccIds.parallelStream()
                .forEach(destinationACId -> {
                    this.accountsService.amountTransfer(AmountTransferDTO.builder()
                            .sourceAccountId(acRichGuyId)
                            .destinationAccountId(destinationACId)
                            .transferAmount(new BigDecimal("10.00"))
                            .build());
                });

        //Test asserts -
        listAccIds.parallelStream()
                .forEach(luckyOnes -> {
                    Account accountStatus = accountsService.getAccount(luckyOnes);
                    assertThat(accountStatus.getBalance().get()).isEqualByComparingTo("20.00");
                });
        Account acLuckyGuy = accountsService.getAccount(acRichGuyId);
        assertThat(acLuckyGuy.getBalance().get()).isEqualByComparingTo("8000.00");
    }

    private String setupTestInsertAccount(String accountPrefix, String initialAmount) {
        String sourceAccountId = accountPrefix + System.currentTimeMillis();
        Account sourceAccount = new Account(sourceAccountId, new BigDecimal(initialAmount));
        this.accountsService.createAccount(sourceAccount);
        return sourceAccountId;
    }
}
