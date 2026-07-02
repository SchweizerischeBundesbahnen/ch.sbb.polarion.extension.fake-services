package ch.sbb.polarion.extension.fake_services.connector;

import ch.sbb.polarion.extension.aad.synchronizer.connector.IGraphConnector;
import ch.sbb.polarion.extension.aad.synchronizer.model.Group;
import ch.sbb.polarion.extension.aad.synchronizer.model.Member;
import ch.sbb.polarion.extension.aad.synchronizer.model.OrganizationData;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakeGraphConnector implements IGraphConnector {

    public static final SecureRandom RANDOM = new SecureRandom();

    private static final String[] NAMES = {
            "Alice", "Bob", "Carol", "David", "Emma", "Eva", "Frank", "Grace", "Henry", "Isabella", "Jack",
            "James", "John", "Olivia", "Sophia", "William", "Alex", "Emily", "Michael", "Sophie"
    };
    private static final String[] SURNAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Garcia", "Martinez", "Lopez",
            "Taylor", "Anderson", "Thomas", "Hernandez", "Moore", "Martin", "Hall", "Allen", "Young", "King",
            "Wright", "Lee", "Scott", "Torres", "Nguyen", "White", "Cruz", "Evans", "Davis", "Carter",
            "Morales", "Anderson", "Gonzalez", "Perez", "Rodriguez", "Cook", "Gomez", "Bell", "Powell", "Rogers"
    };

    @Override
    public List<Group> getGroups(String groupPrefix) {
        final int groupCount = RANDOM.nextInt(5) + 1;

        List<Group> groups = new ArrayList<>();

        for (int i = 0; i < groupCount; i++) {
            groups.add(new Group(groupPrefix + UUID.randomUUID()));
        }

        return groups;
    }

    @Override
    public List<Member> getMembers(String key) {
        final int membersCount = RANDOM.nextInt(15) + 1;
        return generateRandomMembers(membersCount);
    }

    private static List<Member> generateRandomMembers(int numberOfMembers) {
        List<Member> members = new ArrayList<>();

        for (int i = 0; i < numberOfMembers; i++) {
            String displayName = generateRandomName();
            String mail = displayName.toLowerCase().replace(" ", ".") + "@example.com";
            String mailNickname = generateRandomMailNickname();
            members.add(new Member(displayName, mail, mailNickname));
        }

        return members;
    }

    private static String generateRandomName() {
        String name = NAMES[RANDOM.nextInt(NAMES.length)];
        String surname = SURNAMES[RANDOM.nextInt(SURNAMES.length)];
        return name + " " + surname;
    }

    private static String generateRandomMailNickname() {
        String[] prefixes = {"u", "e", "U", "E"};
        String randomNumber = String.format("%06d", RANDOM.nextInt(1000000));
        return prefixes[RANDOM.nextInt(prefixes.length)] + randomNumber;
    }

    @Override
    public OrganizationData getOrganizationData() {
        return new OrganizationData(Instant.now());
    }

}
