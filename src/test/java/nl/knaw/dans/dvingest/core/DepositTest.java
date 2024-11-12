package nl.knaw.dans.dvingest.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

public class DepositTest extends TestDirFixture {

    @Test
    public void ctor_should_throw_IllegalStateException_when_deposit_properties_file_not_found() throws Exception {
        // Given
        var depositDir = testDir.resolve(UUID.randomUUID().toString());
        Files.createDirectories(depositDir);

        // When
        // Then
        assertThatIllegalStateException().isThrownBy(() -> new Deposit(depositDir))
            .withMessage("Error loading deposit properties from " + depositDir.resolve("deposit.properties"));

    }

    @Test
    public void getId_should_return_uuid_from_dir_name() throws Exception {
        // Given
        var uuid = UUID.randomUUID();
        var depositDir = testDir.resolve(uuid.toString());
        Files.createDirectories(depositDir);
        Files.createFile(depositDir.resolve("deposit.properties"));

        // When
        var deposit = new Deposit(depositDir);

        // Then
        assertThat(deposit.getId()).isEqualTo(uuid);
    }

    @Test
    public void deposits_should_be_ordered_by_seqNums() throws Exception {
        // Given
        var id1 = UUID.randomUUID().toString();
        var id2 = UUID.randomUUID().toString();
        var id3 = UUID.randomUUID().toString();

        var deposit1 = testDir.resolve(id1);
        Files.createDirectories(deposit1);
        Files.createFile(deposit1.resolve("deposit.properties"));
        

        // When
        var deposits = List.of(deposit2, deposit3, deposit1);

        // Then
        assertThat(deposits).containsExactly(deposit1, deposit2, deposit3);
    }


}
