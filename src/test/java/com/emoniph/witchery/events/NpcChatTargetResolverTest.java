package com.emoniph.witchery.events;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcChatTargetResolverTest {
    @Test
    void demonicContractNamesFlameImpNotDemon() {
        EnumSet<ChatPersona> personas = namedPersonas("Will this Demonic Contract tame the imp?");

        assertTrue(personas.contains(ChatPersona.FLAME_IMP), personas.toString());
        assertFalse(personas.contains(ChatPersona.DEMON), personas.toString());
    }

    @Test
    void demonBargainNamesDemonNotFlameImp() {
        EnumSet<ChatPersona> personas = namedPersonas("Demon, what is your bargain price?");

        assertTrue(personas.contains(ChatPersona.DEMON), personas.toString());
        assertFalse(personas.contains(ChatPersona.FLAME_IMP), personas.toString());
    }

    @Test
    void mogAndGulgNamesBossNotSmallHobgoblin() {
        EnumSet<ChatPersona> mog = namedPersonas("Mog, answer me.");
        EnumSet<ChatPersona> gulg = namedPersonas("How do I survive Goblin Gulg?");

        assertTrue(mog.contains(ChatPersona.BOSS), mog.toString());
        assertFalse(mog.contains(ChatPersona.HOBGOBLIN), mog.toString());
        assertTrue(gulg.contains(ChatPersona.BOSS), gulg.toString());
        assertFalse(gulg.contains(ChatPersona.HOBGOBLIN), gulg.toString());
    }

    @Test
    void villageGuardNamesGuardNotRegularVillager() {
        EnumSet<ChatPersona> guard = namedPersonas("Village guard, state your business.");
        EnumSet<ChatPersona> nearby = namedPersonas("Any guards nearby?");

        assertTrue(guard.contains(ChatPersona.VILLAGE_GUARD), guard.toString());
        assertFalse(guard.contains(ChatPersona.VILLAGER), guard.toString());
        assertTrue(nearby.contains(ChatPersona.VILLAGE_GUARD), nearby.toString());
        assertFalse(nearby.contains(ChatPersona.HOBGOBLIN), nearby.toString());
    }

    @SuppressWarnings("unchecked")
    private static EnumSet<ChatPersona> namedPersonas(String message) {
        try {
            Method method = NpcChatTargetResolver.class.getDeclaredMethod("explicitlyNamedPersonas", String.class);
            method.setAccessible(true);
            return (EnumSet<ChatPersona>) method.invoke(null, message);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Could not call explicitlyNamedPersonas", ex);
        }
    }
}
