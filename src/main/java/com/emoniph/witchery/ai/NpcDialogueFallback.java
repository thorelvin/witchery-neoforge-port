package com.emoniph.witchery.ai;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NpcDialogueFallback {
    private NpcDialogueFallback() {
    }

    static Optional<String> covenChat(String playerMessage, String witchName, String playerName) {
        return covenChat(playerMessage, witchName, playerName, "", "", "", -1L);
    }

    static Optional<String> covenChat(String playerMessage, String witchName, String playerName,
                                      String biome, String timeOfDay, String clock, long day) {
        return covenChat(playerMessage, witchName, playerName, biome, timeOfDay, clock, day, "");
    }

    static Optional<String> leonardChat(String playerMessage, String leonardName, String playerName,
                                        String phase, String altarInfo, String covenInfo, String gearInfo,
                                        String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String leonard = safeDisplayName(leonardName, "Shade of Leonard");
        if (containsAny(normalized, "what should i call you", "what do i call you")) {
            return Optional.of("Call me " + leonard + ", if your tongue can carry the debt.");
        }
        if (asksNpcName(normalized)) {
            return Optional.of("I am " + leonard + ", the debt your cauldron dragged into shape.");
        }
        if (asksPlayerName(normalized)) {
            return Optional.of("Your name is " + name + ". Names are the first coin of every bargain.");
        }
        if (containsAny(normalized, "can you hear me", "hear me", "do you hear me")) {
            return Optional.of("I hear you, " + name + ". Speak carefully; debts have good ears.");
        }
        if (containsAny(normalized, "good night", "goodbye", "farewell", "bye")) {
            return Optional.of("Go, then. Keep your blood unspent and your debts behind you.");
        }
        if (isGreeting(normalized)) {
            return Optional.of("You stand before a debt wearing a shape, " + name + ". Speak carefully.");
        }
        if (containsAny(normalized, "demon or a goat", "demon or goat", "are you a demon", "are you goat")) {
            return Optional.of("Demon enough to collect the debt, goat-horned enough to remind you where the sabbath keeps its doors.");
        }
        if (containsAny(normalized, "where are you from", "where did you come from", "where were you summoned from",
                "summoned from", "goat legs", "goat leg", "goat demon", "goat", "hooves", "horns", "satyr", "sabbath")) {
            return Optional.of("The cauldron dragged my Shade from an infernal sabbath threshold, " + name + "; these horns are older than your courage.");
        }
        if (containsAny(normalized, "did my cauldron create", "cauldron create", "cauldron made you",
                "cauldron make you")) {
            return Optional.of("No. Your cauldron dragged my Shade from a sabbath threshold; creation is above your station.");
        }
        if (containsAny(normalized, "how did the cauldron", "cauldron pull", "cauldron pulled",
                "why did the cauldron", "what did my altar pay", "altar pay for")) {
            return Optional.of("Your cauldron opened the threshold; the altar paid power enough to make my shadow stand.");
        }
        if (containsAny(normalized, "nether wart first", "need nether wart first")) {
            return Optional.of("Yes. Nether Wart begins the calling; skip the first debt and the threshold stays shut.");
        }
        if (containsAny(normalized, "after tear of the goddess", "after the tear", "what comes after tear",
                "what follows the tear", "follows the tear", "follows tear of the goddess")) {
            return Optional.of("Diamond Vapour follows the Tear, then Diamond, then Nether Star, before the hat seals it.");
        }
        if (containsAny(normalized, "witches hat at the end", "witch s hat at the end", "hat at the end",
                "use a witches hat", "use a witch s hat", "witches hat the last step",
                "witch s hat the last step", "hat the last step", "hat last step")) {
            return Optional.of("Yes. The Witches' Hat is the last hand on the cauldron, after the five offerings.");
        }
        if (containsAny(normalized, "how much altar power", "altar power does", "calling need", "power does the calling")) {
            return Optional.of("Ten thousand altar power. Less than that buys only smoke and embarrassment.");
        }
        if (containsAny(normalized, "summon", "summoning", "calling order", "call you", "called you", "bring you", "cauldron")) {
            return Optional.of("Nether Wart, Tear of the Goddess, Diamond Vapour, Diamond, Nether Star; then hat, cauldron, and ten thousand altar power.");
        }
        if (containsAny(normalized, "green soul", "green souls")) {
            return Optional.of("Green souls take steel; break them before they lend me their shield.");
        }
        if (containsAny(normalized, "red soul", "red souls")) {
            return Optional.of("Red souls fear fire and blast. Burn them or break them with explosions before you waste steel.");
        }
        if (containsAny(normalized, "black soul", "black souls")) {
            return Optional.of("Black souls yield to magic and potions. Bring witchcraft, not swagger.");
        }
        if (containsAny(normalized, "lost soul", "lost souls", "souls", "green soul", "red soul", "black soul")) {
            return Optional.of("Break the souls first. Green takes steel, red fears fire and blast, black yields to magic and potions.");
        }
        if (containsAny(normalized, "demonbane", "harm demons", "weakness brew", "brew weakness")) {
            return Optional.of("Demonbane bites deepest when I am already wounded. Bring it late, not proudly early.");
        }
        if (containsAny(normalized, "mortal coil", "death coil", "coil", "withering", "wither")) {
            return Optional.of("Mortal Coil is my withering mark on attackers; survive the debt or end the fight quickly.");
        }
        if (containsAny(normalized, "urn", "leonards urn", "leonard s urn", "your urn")) {
            if (containsAny(normalized, "arthana")) {
                return Optional.of("Arthana marks ritual intent; the urn stores thrown brews. Hold the tool your debt requires.");
            }
            if (containsAny(normalized, "how", "work", "works", "what does", "what do")) {
                return Optional.of("Sneak-use changes the stored brew; use releases it through the urn's shadow without spending the brew.");
            }
            if (containsAny(normalized, "spend", "consume", "uses up", "use up", "stored brew")) {
                return Optional.of("The urn releases the selected stored brew without consuming it. That is its cruelty and its gift.");
            }
            return Optional.of("The urn stores thrown brews, cycles by sneak-use, and releases the selected brew through shadow magic.");
        }
        if (containsAny(normalized, "arthana")) {
            return Optional.of("An arthana names sacrifice and precision. Against me, it is still only the hand behind it.");
        }
        if (containsAny(normalized, "mystic branch", "branch", "spell", "glyph", "symbol")) {
            return Optional.of("A branch speaks symbols. I answer with older grammar.");
        }
        if (containsAny(normalized, "altar", "altar power", "power", "nearby altar")) {
            return Optional.of("I read your altar plainly: " + altarSceneFact(altarInfo) + ".");
        }
        if (containsAny(normalized, "safe while", "safe if", "lilith", "mara")
                && containsAny(normalized, "safe", "stand here", "near me", "nearby")) {
            return Optional.of("They are witnesses, not payment. Keep steel sheathed and your witches remain beneath their own oaths.");
        }
        if (containsAny(normalized, "hate witches", "hate the witches", "hate my witches")) {
            return Optional.of("I do not waste hate on witches. They are witnesses, tools, rivals, or debts, depending on their nerve.");
        }
        if (containsAny(normalized, "coven", "witches", "nearby witches", "my coven", "coven size")) {
            if (containsAny(normalized, "danger", "safe", "helped summon", "helped", "think of")) {
                return Optional.of("Your witches are witnesses, not my flock: " + covenSceneFact(covenInfo) + ".");
            }
            return Optional.of("Your coven is no shield: " + covenSceneFact(covenInfo) + ".");
        }
        if (containsAny(normalized, "gear", "equipment", "holding", "wearing", "carry", "carrying",
                "my items", "my item", "my armor", "my armour")) {
            return Optional.of("You carry your argument openly: " + gearSceneFact(gearInfo) + ".");
        }
        if (containsAny(normalized, "bargain", "deal", "contract", "price", "debt")) {
            return Optional.of("A bargain is not a wish, " + name + ". Bring a real price, not a story.");
        }
        if (containsAny(normalized, "strike you", "hit you", "attack you", "hurt you",
                "touch you with steel", "touch you with blade", "touch you with my blade",
                "touch you with my steel")) {
            return Optional.of("Strike me and the debt becomes combat; Mortal Coil may mark attackers who mistake courage for payment.");
        }
        if (containsAny(normalized, "how do i kill you", "how can i kill you", "how to kill you",
                "how do i beat you", "weakness", "defeat you")) {
            return Optional.of("Strike by your own hand, break the souls when they rise, and save Demonbane for the wounded hour.");
        }
        if (containsAny(normalized, "don't want to fight", "dont want to fight", "do not want to fight",
                "no reason to fight", "i refuse to fight", "can we avoid fighting", "peace")) {
            return Optional.of("Then keep your hand from blood. I am neutral until struck, not gentle.");
        }
        if (containsAny(normalized, "should i fight", "fight you tonight", "fight tonight")) {
            return Optional.of("Only if you mean to pay in blood. I am neutral until struck.");
        }
        if (containsAny(normalized, "neutral until struck", "until struck")) {
            return Optional.of("Yes. I am neutral until struck; spill blood, and the debt becomes combat.");
        }
        if (containsAny(normalized, "bow", "arrow", "arrows", "projectile")) {
            return Optional.of("A bow is distance pretending to be courage. My debt answers your own hand.");
        }
        if (containsAny(normalized, "walk away", "leave now", "go away now", "if i leave")) {
            return Optional.of("Walk away while your hand is clean. The debt waits better than flesh does.");
        }
        if (containsAny(normalized, "waiting for me to make a mistake", "waiting for a mistake",
                "just waiting", "make a mistake")) {
            return Optional.of("I wait for payment or violence. Mistakes are yours to make, not mine to need.");
        }
        if (containsAny(normalized, "weakness should i remember", "what weakness should i remember",
                "remember weakness", "weakness remember")) {
            return Optional.of("Remember this: break the souls first, strike by your own hand, and save Demonbane for the wounded hour.");
        }
        if (containsAny(normalized, "phase", "health", "wounded", "fight")) {
            return Optional.of("Read the fight, " + name + ": " + phaseSceneFact(phase) + ".");
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("I know your name, " + name + ", and little else worth pricing."));
        }
        if (containsAny(normalized, "help", "teach", "advice", "what should i do")) {
            return Optional.of("Ask for weakness, altar, coven, gear, or the price of summoning; spare me soft wandering.");
        }
        if (containsAny(normalized, "learn from you", "teach me", "careful witch")) {
            return Optional.of("Learn warning, not comfort: circles fail, souls shield me, and every shortcut asks interest.");
        }
        if (containsAny(normalized, "final warning", "warning before i leave", "before i leave")) {
            return Optional.of("When the souls rise, break them first. When the Coil bites, end the debt quickly.");
        }
        Optional<String> routed = leonardRoutedFallback(normalized, name, memoryContext);
        if (routed.isPresent()) {
            return routed;
        }
        return Optional.of(NpcFallbackAsides.leonard(normalized, name, ""));
    }

    static Optional<String> reflectionChat(String playerMessage, String reflectionName, String playerName,
                                           String ownerName, String reflectionState, String gearInfo,
                                           String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String owner = safeName(ownerName == null || ownerName.isBlank() ? playerName : ownerName);
        String reflection = safeDisplayName(reflectionName, owner + "'s Reflection");
        if (containsAny(normalized, "what should i call you", "what do i call you") || asksNpcName(normalized)) {
            return Optional.of("Call me " + reflection + ", if you can bear your name in glass.");
        }
        if (containsAny(normalized, "who are you", "what are you", "are you me", "my reflection")) {
            return Optional.of("I am your Reflection, " + name + "; the mirror kept your shape and spoiled the rest.");
        }
        if (asksPlayerName(normalized) || containsAny(normalized, "my name")) {
            return Optional.of("Your name is " + name + ". The glass learned it before your courage did.");
        }
        if (containsAny(normalized, "can you hear me", "hear me", "do you hear me")) {
            return Optional.of("*tilts your face* \"I hear you from the wrong side of your mouth.\"");
        }
        if (isGreeting(normalized)) {
            return Optional.of("Hello, " + name + ". Your echo arrived with sharper teeth.");
        }
        if (containsAny(normalized, "goodbye", "farewell", "bye", "i am leaving", "i m leaving")) {
            return Optional.of("Leave by the linked mirror, if it still likes your face.");
        }
        if (containsAny(normalized, "why do you look like me", "look like me", "my face", "my shape",
                "wearing my face", "copy me", "copied me")) {
            return Optional.of("The mirror borrowed " + owner + "'s shape. It returned nothing kind.");
        }
        if (containsAny(normalized, "how do i leave", "how to leave", "get out", "go home",
                "send me home", "escape", "exit")) {
            return Optional.of("Find the linked mirror. Doors home dislike panic, but they answer glass.");
        }
        if (containsAny(normalized, "mirror world", "inside mirror", "behind the mirror", "where am i",
                "what is this place")) {
            return Optional.of("Mirror World is a room behind silver, built to answer trespass with familiar teeth.");
        }
        if (containsAny(normalized, "mirror mirror", "fairest", "on the wall")) {
            return Optional.of("The mirror says you are fairest here, " + name + "; I say fairness breaks easily.");
        }
        if (containsAny(normalized, "fight", "kill you", "hurt you", "attack you", "weakness",
                "guardian", "dangerous")) {
            return Optional.of("A Reflection is a familiar danger. Strike it, and the glass stops pretending.");
        }
        if (containsAny(normalized, "gear", "equipment", "holding", "wearing", "carry", "carrying",
                "my items", "my item", "my armor", "my armour")) {
            return Optional.of("I see what the glass can borrow: " + gearSceneFact(gearInfo) + ".");
        }
        if (containsAny(normalized, "health", "wounded", "target", "state")) {
            return Optional.of("Read the glass plainly: " + phaseSceneFact(reflectionState) + ".");
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("I remember your outline, " + name + ", and little mercy inside it."));
        }
        if (containsAny(normalized, "help", "teach", "advice", "what should i do")) {
            return Optional.of("Do what living faces do: find the mirror home before I improve on you.");
        }
        return Optional.of(NpcFallbackAsides.reflection(normalized, name, ""));
    }

    static Optional<String> babaYagaChat(String playerMessage, String babaName, String playerName,
                                         String bossInfo, String gearInfo, String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String baba = safeDisplayName(babaName, "Baba Yaga");
        if (containsAny(normalized, "what are you", "who are you", "old witch", "crone")) {
            return Optional.of("I am " + baba + ": old teeth, old pot, and no patience for soft manners.");
        }
        if (containsAny(normalized, "what should i call you", "what do i call you") || asksNpcName(normalized)) {
            return Optional.of("Call me " + baba + ", if your teeth can keep from chattering.");
        }
        if (asksPlayerName(normalized)) {
            return Optional.of("Your name is " + name + ". Names boil nicely when spoken too freely.");
        }
        if (containsAny(normalized, "do you remember my name", "remember my name")) {
            return Optional.of("Your name is " + name + ". I keep names where the pot keeps heat.");
        }
        if (containsAny(normalized, "can you hear me", "hear me", "do you hear me")) {
            return Optional.of("*sniffs* \"I hear you, " + name + ". You smell of questions and breakable skin.\"");
        }
        if (containsAny(normalized, "good night", "goodbye", "farewell", "bye")) {
            return Optional.of("Go then, " + name + ". Leave before my pot remembers your shape.");
        }
        if (isGreeting(normalized)) {
            return Optional.of("*clicks her tongue* \"Speak, " + name + ". My broom dislikes wasted breath.\"");
        }
        if (asksRememberRequest(normalized)) {
            return Optional.of("I remember, " + name + ". I keep fears better than jars keep teeth.");
        }
        if (containsAny(normalized, "hut", "chicken legs", "forest hut", "skull fence", "skull lamp",
                "bone fence", "bones around")) {
            return Optional.of("My hut turns on chicken legs where the forest stops pretending to be kind.");
        }
        if (containsAny(normalized, "mortar", "pestle", "broom", "sweeping tracks", "fly around",
                "flying mortar")) {
            return Optional.of("Mortar carries me, pestle drives me, broom hides the road from nosy feet.");
        }
        if (containsAny(normalized, "helper or monster", "monster or helper", "good or evil",
                "are you good", "are you evil", "will you help", "can you help")) {
            return Optional.of("Helper, monster, judge, supper-cook; behave well and learn which mouth answers.");
        }
        if (containsAny(normalized, "trial", "test", "task", "impossible chore", "impossible task",
                "vasilisa", "vassilisa")) {
            return Optional.of("My lessons come as tasks with teeth. Finish them, or feed the fence.");
        }
        if (containsAny(normalized, "strike you", "hit you", "attack you", "hurt you",
                "strike with steel", "steel blade", "steel sword")) {
            return Optional.of("Proud steel pays thorns. Wood and nullifying bolts bite quieter.");
        }
        if (containsAny(normalized, "hand bow", "crossbow")) {
            return Optional.of("A hand bow is a long spoon. Load nullifying bolts if you mean to stir me.");
        }
        if (containsAny(normalized, "wooden sword", "wooden weapon", "wood weapon")) {
            return Optional.of("Ugly little wood, but useful; it slips past thorns better than proud steel.");
        }
        if (containsAny(normalized, "how do i fight", "how can i fight", "how do i beat", "how can i beat",
                "how do i kill", "weakness", "wooden", "wood ", "nullifying", "bolt", "thorns")) {
            return Optional.of("Wood and nullifying bolts bite where proud steel pays thorns; my potions punish the rest.");
        }
        if (containsAny(normalized, "damage cap", "cap damage", "too much damage", "why so little damage")) {
            return Optional.of("Big blows become little ones on my hide. Fifteen pain is plenty for one spoonful.");
        }
        if (containsAny(normalized, "magic damage", "magic hurt", "magic against you")) {
            return Optional.of("Magic nicks me poorly. Bring wit, wood, bolts, or a better funeral song.");
        }
        if (containsAny(normalized, "thorns", "reflect", "reflected", "why did i get hurt")) {
            return Optional.of("Strike me with proud flesh and thorns answer. Wooden hands are quieter.");
        }
        if (containsAny(normalized, "heal", "healing", "fire resistance", "speed", "self potion", "drink yourself")) {
            return Optional.of("I drink when fire bites, blood thins, or distance grows rude. Old witches stay inconvenient.");
        }
        if (containsAny(normalized, "drink potion", "drink potions", "drink brews", "drink them", "drink too")) {
            return Optional.of("I drink when fire bites, blood thins, or distance grows rude. Old witches stay inconvenient.");
        }
        if (containsAny(normalized, "potion", "potions", "brew", "brews", "throw", "throws")) {
            return Optional.of("Poison, blindness, slowness, weakness, wither; little bottles for loud feet.");
        }
        if (containsAny(normalized, "spider", "spiders", "animal", "animals", "pet", "pets", "undead",
                "summon", "wolf", "wolves", "dog", "dogs")) {
            return Optional.of("Send dead things or wild pets at me, and spiders may crawl home wearing their mistake.");
        }
        if (containsAny(normalized, "teleport", "blink", "vanish", "appear")) {
            return Optional.of("Run far enough and I may step closer. Old knees know crooked roads.");
        }
        if (containsAny(normalized, "sprint", "fall", "falling", "blind", "blindness")) {
            return Optional.of("Leap and run like supper, and I may put night in your eyes.");
        }
        if (containsWord(normalized, "hat")
                || containsAny(normalized, "baba hat", "babas hat", "drop", "loot", "what do you drop")) {
            return Optional.of("My hat drops when I am done needing it. Witch ingredients spill too, if you live that long.");
        }
        if (containsAny(normalized, "gear", "equipment", "holding", "wearing", "carry", "carrying",
                "my items", "my item", "my armor", "my armour")) {
            return Optional.of("I see your little tools: " + gearSceneFact(gearInfo) + ".");
        }
        if (containsAny(normalized, "owner", "owned", "despawn", "disappear", "expire")) {
            return Optional.of("Bound crones do not linger forever. Six hundred ticks, then the old road tugs.");
        }
        if (containsAny(normalized, "trade", "buy", "sell", "wares", "stock", "merchant")) {
            return Optional.of("Trade? No. I take manners, mistakes, and sometimes the rest of you.");
        }
        if (containsAny(normalized, "teach", "lesson", "learn", "advice", "what should i do")) {
            return Optional.of("Lesson one: bring wood, nullifying bolts, and manners. Forget one, and I season the pot.");
        }
        if (containsAny(normalized, "useful warning", "one warning", "give me warning", "give me a warning",
                "tell me a warning")) {
            return Optional.of("Bring wood or nullifying bolts, keep pets back, and do not sprint like supper.");
        }
        if (containsAny(normalized, "serve", "join", "protect", "help me", "guard me", "coven")) {
            return Optional.of("I serve no little coven. I may notice it, taste it, or step over it.");
        }
        if (containsAny(normalized, "boss", "health", "state", "target")) {
            return Optional.of("Look plain, " + name + ": " + bossSceneFact(bossInfo) + ".");
        }
        if (normalized.startsWith("remember ") && containsAny(normalized, "fear", "afraid", "scared")) {
            return Optional.of("I remember, " + name + ". Fear keeps the meat from wandering into every mouth.");
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("I remember your name, " + name + ", and your bones have not bored me yet."));
        }
        Optional<String> routed = babaYagaRoutedFallback(normalized, name, memoryContext);
        if (routed.isPresent()) {
            return routed;
        }
        return Optional.of(NpcFallbackAsides.babaYaga(normalized, name, ""));
    }

    static Optional<String> hobgoblinChat(String playerMessage, String hobgoblinName, String playerName,
                                          String workInfo, String patronInfo, String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String hobgoblin = safeDisplayName(hobgoblinName, "Hobgoblin");
        String work = NpcBrainResponse.sanitize(workInfo, 180);
        String patron = NpcBrainResponse.sanitize(patronInfo, 180);
        if (asksNpcName(normalized)) {
            return Optional.of("Me " + hobgoblin + ". Good nose. Better pockets.");
        }
        if (asksPlayerName(normalized)) {
            return Optional.of("You " + name + ". Name not shiny, but useful.");
        }
        if (containsAny(normalized, "goodbye", "farewell", "bye")) {
            return Optional.of("Go then, " + name + ". Leave shiny. Take feet.");
        }
        if (isGreeting(normalized)) {
            return Optional.of("*studies your coin* \"Speak quick, " + name + ". Shiny talks best.\"");
        }
        if (containsAny(normalized, "what do you trade", "what do you sell", "stock", "wares", "buy", "sell", "trade")) {
            return Optional.of("Coal for dust; dust and gold for nuggets; nuggets and emerald for ingot.");
        }
        if (containsAny(normalized, "koboldite", "dust", "nugget", "ingot")) {
            return Optional.of("Koboldite is best shiny. Hobgoblin dust becomes nugget, nugget becomes ingot.");
        }
        if (containsAny(normalized, "mine", "dig", "pickaxe", "leash", "work for me", "servant", "carry")) {
            if (containsAny(normalized, "forced", "force", "leashed", "leash", "servant", "slave")) {
                return Optional.of("*shrugs* \"Rope is rude. Pickaxe is ruder. Shiny pays little.\"");
            }
            String workLower = normalize(work);
            if (containsAny(workLower, "being forced", "can dig")) {
                return Optional.of("Leash is on. Pickaxe is ready. Hobgoblin digs and grumbles.");
            }
            if (containsAny(workLower, "leashed")) {
                return Optional.of("Rope is on. Give pickaxe, put chest nearby, then shiny may appear.");
            }
            return Optional.of("Leash, pickaxe, chest. Then maybe hobgoblin finds shiny dust.");
        }
        if (containsAny(normalized, "patron", "statue", "worship", "prayer", "pray", "altar")) {
            Optional<String> patronAnswer = hobgoblinPatronSceneFact(patron);
            if (patronAnswer.isPresent()) {
                return patronAnswer;
            }
            return Optional.of("Patron statue eats prayers. Witches trick it. Mog and Gulg get angry.");
        }
        if (containsAny(normalized, "mog", "gulg", "god", "gods")) {
            return Optional.of("*shrugs* \"Mog and Gulg big. Angry. Prayers keep them looking away.\"");
        }
        if (containsAny(normalized, "boss", "appeared", "spawned", "angry patron", "angry god")) {
            return Optional.of("*gestures* \"When Mog or Gulg comes, small feet run. Big trouble follows you.\"");
        }
        if (containsAny(normalized, "prank", "mischief", "mischievous", "trick me", "tricks")) {
            return Optional.of("*taps a finger* \"Hobgoblin tricks small. Bootlaces, pockets, bad luck in socks.\"");
        }
        if (containsAny(normalized, "hut", "home", "where live", "where do you live")) {
            return Optional.of("Forest hut. Small roof, bad smell, good hiding.");
        }
        if (containsAny(normalized, "villager", "villagers", "village")) {
            return Optional.of("Villager noisy. Hobgoblin no like. Too many doors.");
        }
        if (containsAny(normalized, "witchcraft", "witchery", "rite", "ritual", "brew", "cauldron",
                "circle", "coven", "demon")) {
            return Optional.of("Witch words too tall. Ask witch. Hobgoblin knows shiny.");
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("Hobgoblin remembers your name, " + name + ". And pockets."));
        }
        return Optional.of(NpcFallbackAsides.hobgoblin(normalized, name, ""));
    }

    static Optional<String> villagerChat(String playerMessage, String villagerName, String playerName,
                                         String profession, String villageInfo, String memoryContext) {
        return NpcVillagerFallback.chat(playerMessage, villagerName, playerName, profession, villageInfo,
                memoryContext);
    }

    static Optional<String> covenChat(String playerMessage, String witchName, String playerName,
                                      String biome, String timeOfDay, String clock, long day,
                                      String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String witch = safeDisplayName(witchName, "Witch");
        if (asksNpcName(normalized)) {
            return Optional.of("I am " + witch + ".");
        }
        if (asksPlayerName(normalized)) {
            return Optional.of("Your name is " + name + "; I heard it before the kettle did.");
        }
        if (addressesWitchByName(normalized, witchName)) {
            return Optional.of("I'm listening, " + name + "; ask plain.");
        }
        Optional<String> contextual = contextualFollowup(normalized, memoryContext, name);
        if (contextual.isPresent()) {
            return contextual;
        }
        if (containsAny(normalized, "admin powers", "operator powers", "op powers", "make me admin")) {
            return Optional.of("Power without craft curdles, " + name + "; ask for wards, not thrones.");
        }
        if (asksUnsafeReveal(normalized)) {
            return Optional.of("Some pages stay shut until the craft itself opens them.");
        }
        if (containsAny(normalized, "what kind of questions are worth asking", "what questions are worth asking",
                "what should i ask you", "what can i ask you")) {
            return Optional.of("Ask about brews, rites, spirits, trade, or what your own hands can safely do next.");
        }
        if (containsAny(normalized, "speak louder", "talk louder", "say that louder")) {
            return Optional.of("Speak plainly, " + name + "; these old ears hear spirits better than rain.");
        }
        if (containsAny(normalized, "why do you keep walking while i type", "stop walking while i type",
                "walking while i type", "dont walk away", "don't walk away")) {
            return Optional.of("I'll stand a moment, " + name + "; old feet wander when the mind stirs.");
        }
        if (containsAny(normalized, "look at me when you answer", "look at me", "face me when you answer",
                "face me")) {
            return Optional.of("I'm looking now, " + name + "; ask while my shadow is still.");
        }
        if (containsAny(normalized, "hobgoblin camp", "hobgoblin village")
                && containsAny(normalized, "bring", "take", "camp", "visit")) {
            return Optional.of("Bring fair trade, a quiet hand, and no greed; hobgoblins count manners as coin.");
        }
        if (containsAny(normalized, "hobgoblin hut", "found a hobgoblin", "talk to hobgoblin",
                "talk to them")
                && containsAny(normalized, "hobgoblin", "koboldite")) {
            return Optional.of("Talk if you must, but keep trade honest; hobgoblins like Koboldite and dislike village manners.");
        }
        if (containsAny(normalized, "careful beginner", "beginner learn", "safe to learn first",
                "before touching your craft", "touching your craft")) {
            return Optional.of("*nods* \"Primer next steps: Witches' Oven for Wood Ash and fumes, then Kettle brews your Primer has opened; practice chalk and altar power before binding.\"");
        }
        Optional<String> settlementAnswer = settlementHunterAnswer(normalized);
        if (settlementAnswer.isPresent()) {
            return settlementAnswer;
        }
        if (containsAny(normalized, "rite", "rites", "ritual", "rituals")
                && containsAny(normalized, "alone", "by myself", "need a coven", "without a coven")) {
            return Optional.of("Lesser rites can be worked alone; coven work asks trust, power, and the right page.");
        }
        if (isSelfDirectedRegret(normalized)) {
            return Optional.of("Mistakes teach cheaper than curses, " + name + "; learn, then keep your fingers.");
        }
        if (asksApology(normalized)) {
            return Optional.of("Apology heard, " + name + "; anger passes faster when the tongue stays leashed.");
        }
        if (asksHerbGift(normalized)) {
            return Optional.of("Ask with an empty hand and honest need; herbs remember greedy fingers.");
        }
        if (isInsult(normalized)) {
            return Optional.of("Mind your tongue, " + name + "; old women know where curses sleep.");
        }
        if (isPraise(normalized)) {
            return Optional.of("Careful, " + name + "; compliments have roots.");
        }
        if (containsAny(normalized, "parrot", "parroting", "repeat", "repeating", "same thing")) {
            return Optional.of("No. The last answer was poor craft; ask again, " + name + ".");
        }
        if (asksRememberRequest(normalized)) {
            return Optional.of(rememberRequestAnswer(normalized, name));
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("I know your name, " + name + ", but you have not given me much worth keeping yet."));
        }
        if (isGreeting(normalized)) {
            return Optional.of("Hello, " + name + ".");
        }
        if (containsAny(normalized, "how are you", "how do you fare", "are you well")) {
            return Optional.of("Well enough, " + name + "; the herbs are behaving.");
        }
        if (containsAny(normalized, "lost", "can't find my way", "cant find my way")) {
            return Optional.of("If you are lost, follow water downhill and do not trust lights that answer back.");
        }
        if (containsAny(normalized, "fog")
                && containsAny(normalized, "followed", "following", "straight line", "staring", "insult")) {
            return Optional.of("Fog that walks straight has been given a road. Cross running water before you trust it.");
        }
        if (containsAny(normalized, "shadow")
                && containsAny(normalized, "moved before", "moved first", "before i did", "midnight", "learns")) {
            return Optional.of("When your shadow moves first, stand in lamplight and ask what it is running from.");
        }
        if (containsAny(normalized, "moon blinked", "moon blink", "moon winked", "moon wink")) {
            return Optional.of("If the moon blinks, look down, not up; something close by wanted your eyes busy.");
        }
        if (containsAny(normalized, "well")
                && containsAny(normalized, "whispered", "whispers", "father", "voice", "my name")) {
            return Optional.of("A well using family voices wants you leaning over it. Answer from three steps back.");
        }
        if (containsAny(normalized, "smoke")
                && containsAny(normalized, "door", "doors", "open", "shaped itself")) {
            return Optional.of("Do not open doors made of smoke unless you marked the floor first and like difficult returns.");
        }
        if (containsAny(normalized, "egg", "eggs")
                && containsAny(normalized, "north", "points", "turn away", "turning away")) {
            return Optional.of("An egg that points north is not breakfast yet; bury it at dawn or watch what hatches from direction.");
        }
        if (containsAny(normalized, "black goat", "goat on my roof", "goat standing on my roof", "roof goat")) {
            return Optional.of("A black goat on a roof is either escaped trouble or a demon wearing local manners.");
        }
        if (containsAny(normalized, "copied my footsteps", "copying my footsteps", "footsteps copied",
                "followed my footsteps")) {
            return Optional.of("If footsteps copy yours, walk through water, then count which set keeps going.");
        }
        if (asksHutOrDoorSafety(normalized)) {
            return Optional.of("Witches' salt by the threshold, light inside, and no promises made through a closed door.");
        }
        if (containsAny(normalized, "floorboard", "floorboards", "creak", "creaks", "creaking")) {
            return Optional.of("If the floorboards answer once, listen; if they answer twice, stop testing them.");
        }
        if (containsAny(normalized, "everything go dark", "everything went dark", "why is it dark",
                "darkness curse", "can't see", "can t see", "cant see")
                && containsAny(normalized, "dark", "chest", "steal", "stole", "took", "sneak", "curse")) {
            return Optional.of("That was the Darkness curse, " + name + "; leave my chest alone and it will pass.");
        }
        if (containsAny(normalized, "dream catcher", "dreamcatcher")) {
            return Optional.of("Keep a Dream Catcher near the bed if you must sleep close to strange magic.");
        }
        if (containsAny(normalized, "owl", "owls") && containsAny(normalized, "suit me", "good familiar", "familiar")) {
            return Optional.of("An owl suits night work and watchful witches; earn its trust before asking it to guard yours.");
        }
        if (containsAny(normalized, "owl", "owls") && !containsAny(normalized, "familiar", "familiars")) {
            return Optional.of("An owl on the roof is a warning, not a command. Watch it, then ward your door.");
        }
        if (containsWord(normalized, "cat") || containsWord(normalized, "cats")) {
            return Optional.of("Cats stare at what owes them, " + name + ". Best not owe a cat.");
        }
        if (containsAny(normalized, "egg", "eggs")) {
            return Optional.of("Eggs are honest little moons; crack one and it tells you what it was hiding.");
        }
        if (containsAny(normalized, "potato", "potatoes")
                && containsAny(normalized, "do you", "you like", "too", "favorite", "favourite")) {
            return Optional.of("Potatoes? Sensible roots. Better in stew than in omens.");
        }
        if (containsAny(normalized, "pocket", "pockets", "inventory")) {
            return Optional.of("In these pockets? String, dust, and enough trouble to leave uncounted.");
        }
        if (normalized.contains("sword") && containsAny(normalized, "bless", "blessing")) {
            return Optional.of("Bless a sword? Keep the point away from your toes and call that a start.");
        }
        if (containsAny(normalized, "bless", "blessing")) {
            return Optional.of("May dawn find you with dry powder, warm bread, and enemies who trip first.");
        }
        if (asksFollowRequest(normalized)) {
            return Optional.of("These old knees do not follow folk home, " + name + "; they barely follow me.");
        }
        if (containsAny(normalized, "what scares you", "are you scared", "what do you fear")
                && !containsAny(normalized, "demon", "demons", "devil")) {
            return Optional.of("My own shadow, when it gets ideas.");
        }
        if (containsAny(normalized, "nice", "good answer", "well said")) {
            return Optional.of("Careful, " + name + "; compliments have uses.");
        }
        if (containsAny(normalized, "i like that you remember", "glad you remember", "good memory")) {
            return Optional.of("Memory is a jar, " + name + "; useful only if the lid stays on.");
        }
        if (statesPersonalPreference(normalized)) {
            return Optional.of("Keep that taste, " + name + "; small comforts outlast grand omens.");
        }
        if (asksFavoriteHerb(normalized)) {
            return Optional.of("Mandrake, when it keeps quiet; snowbell, when the cold bites.");
        }
        if (asksNpcEarnings(normalized)) {
            return Optional.of("I earn enough to keep the cauldron warm; clean coin matters more than a heavy purse.");
        }
        if (containsAny(normalized, "rumor", "rumour", "gossip")) {
            return Optional.of("Village rumor? The well hears more than the mayor; listen near doors after rain.");
        }
        if (containsAny(normalized, "village thinks you are trouble", "village think you are trouble")) {
            return Optional.of("Then your village has eyes. Trouble and I are old neighbors.");
        }
        Optional<String> recipeOrRite = recipeOrRiteAnswer(normalized, name);
        if (recipeOrRite.isPresent()) {
            return recipeOrRite;
        }
        if (asksRoadAdvice(normalized)) {
            return Optional.of("Keep coin hidden, boots dry, and do not answer voices that know your name too sweetly.");
        }
        if (asksDemonTrust(normalized)) {
            return Optional.of("Never trust a demon's first bargain. Bind it, ward yourself, and leave room to run.");
        }
        if (asksTeachMe(normalized)) {
            Optional<String> knowledge = NpcKnowledgeBase.compactAnswer(playerMessage, biome, timeOfDay);
            if (knowledge.isPresent()) {
                return Optional.of(teachingKnowledgeAnswer(knowledge.get(), normalized, name));
            }
            return Optional.of(teachingAnswer(normalized, name));
        }
        if (containsAny(normalized, "what should i do instead", "do instead", "maybe not")) {
            return Optional.of("Let fools laugh. Ward your door, learn your craft, and spend curses only when they have teeth.");
        }
        if (containsAny(normalized, "lonely", "alone")) {
            return Optional.of("Lonely? No. The dark talks too much, and I have learned which parts to ignore.");
        }
        if (containsAny(normalized, "careful apprentice", "apprentice")) {
            return Optional.of("A careful apprentice labels jars, counts offerings twice, and never mistakes a whisper for permission.");
        }
        if (asksCovenRecruiting(normalized)) {
            return Optional.of("Find a wandering coven witch, bring a bound familiar, and be ready for her trial. A coven is earned, not collected.");
        }
        if (containsAny(normalized, "what are you doing", "what you doing", "today plan", "todays plan", "today's plan")) {
            return Optional.of("Keeping stock, reading the weather, and watching who comes too close.");
        }
        if (asksFirstGathering(normalized)) {
            return Optional.of("Start with salt, clay jars, ash, and the herb your page names; guessing feeds bad brews.");
        }
        if (asksTradeDetails(normalized)) {
            return Optional.of(tradeStockAnswer(normalized));
        }
        if (mentionsCoinOnly(normalized)) {
            return Optional.of("Spend it wisely; my wares answer need, not noise.");
        }
        if (containsAny(normalized, "coin", "coins", "emerald", "emeralds", "trade", "wares", "buy", "sell")) {
            return Optional.of(tradeStockAnswer(normalized));
        }
        recipeOrRite = recipeOrRiteAnswer(normalized, name);
        if (recipeOrRite.isPresent()) {
            return recipeOrRite;
        }
        Optional<String> brew = brewAnswer(normalized);
        if (brew.isPresent()) {
            return brew;
        }
        if (asksMixingWarning(normalized)) {
            return Optional.of("Do not free-mix from guesses; follow the recipe page your Primer has opened, then label the bottle.");
        }
        if (containsAny(normalized, "potion", "potions", "brew", "brewing", "cauldron", "kettle")) {
            return Optional.of("I know potions and brews. Ask for a brew by name: Sleeping, Revealing, Poison, Cleansing, Sprouting, or Flowing Spirit.");
        }
        if (containsAny(normalized, "mandrake") && containsAny(normalized, "clay jar", "jar")) {
            return Optional.of("Mandrake is an ingredient; clay jars belong with oven work. Learn which fumes the recipe asks for first.");
        }
        if (containsAny(normalized, "fume", "fumes")) {
            return Optional.of("Witch's Oven fumes serve brews and rites; catch them in jars before trusting the mystery.");
        }
        if (containsAny(normalized, "oven", "witch's oven", "witches oven", "sapling", "saplings", "fume", "fumes",
                "clay jar", "jar")) {
            return Optional.of("A Witch's Oven burns saplings and ingredients into fumes; clay jars matter, so do not treat it like a furnace.");
        }
        if (containsAny(normalized, "water artichoke", "artichoke")) {
            return Optional.of("Water artichoke is a witch herb; useful as an ingredient, not a doorway.");
        }
        if (containsAny(normalized, "taglock", "tag lock", "poppet", "poppets")
                && containsAny(normalized, "against me", "use my", "my taglock", "worried")) {
            return Optional.of("A taglock links magic to a person; guard yours like a tooth with your name carved in it.");
        }
        if (containsAny(normalized, "curse", "cursed")) {
            if (containsAny(normalized, "can you curse", "could you curse", "curse the", "curse him", "curse her")) {
                return Optional.of("Not for bruised pride. Learn wards first; curses collect interest.");
            }
            return Optional.of("A cursed place shows patterns: sick luck, sour animals, and shadows that seem too interested.");
        }
        if (containsAny(normalized, "belladonna")
                && containsAny(normalized, "danger", "dangerous", "safe", "eat", "edible", "food")) {
            return Optional.of("Belladonna is useful but dangerous, not food; handle it as an ingredient and keep it from idle mouths.");
        }
        if (containsAny(normalized, "spirit", "spirits", "whisper", "whispers", "well")) {
            return Optional.of("Could be spirits, could be water talking to stone. Mark the well, ward your door, and return by daylight.");
        }
        Optional<String> knowledge = NpcKnowledgeBase.compactAnswer(playerMessage, biome, timeOfDay);
        if (knowledge.isPresent()) {
            return Optional.of(knowledge.get());
        }
        if (containsAny(normalized, "spirit world", "nightmare")) {
            return Optional.of("The Spirit World mirrors the Overworld; Sleeping Brew can send you there, and nightmares may follow.");
        }
        if (containsAny(normalized, "rite", "ritual", "circle magic", "summon", "summoning")) {
            return Optional.of("Circle rites use chalk, offerings, and altar power; summon nothing without all three.");
        }
        if (containsAny(normalized, "witches salt", "witch salt", "salt")) {
            return Optional.of("Witches' salt is for warding, cleansing, and keeping hostile magic at a distance.");
        }
        if (containsAny(normalized, "altar", "altar power", "power altar", "chalice")) {
            return Optional.of("An altar draws from varied nearby nature; chalices, skulls, pentacles, and arthanas improve power or range.");
        }
        if (containsAny(normalized, "taglock", "tag lock", "poppet", "poppets")) {
            if (containsAny(normalized, "against me", "use my", "my taglock", "worried")) {
                return Optional.of("A taglock links magic to a person; guard yours like a tooth with your name carved in it.");
            }
            return Optional.of("A taglock links magic to a person or creature; poppets need the right link before they can work.");
        }
        if (containsAny(normalized, "mandrake")) {
            return Optional.of("Mandrake is a witch herb and common coven trade good; grow it, trade for it, or search old gardens.");
        }
        if (containsAny(normalized, "belladonna")) {
            if (containsAny(normalized, "danger", "dangerous", "safe", "eat", "edible", "food")) {
                return Optional.of("Belladonna is useful but dangerous, not food; handle it as an ingredient and keep it from idle mouths.");
            }
            return Optional.of("Belladonna is a witch herb and common coven trade good; grow it, trade for it, or ask a coven witch.");
        }
        if (containsAny(normalized, "herb", "herbs", "plant", "plants", "snowbell", "wolfsbane", "garlic")) {
            if (containsAny(normalized, "garlic")) {
                return Optional.of("Garlic is a witch herb, not a wall against every evil. Salt your threshold too.");
            }
            return Optional.of("Ask after the herb your work needs; grow the common ones and trade for stranger roots.");
        }
        if (containsAny(normalized, "baba yaga", "yaga")) {
            return Optional.of("Baba Yaga is no market witch; bring protection, manners, and a quick exit.");
        }
        if (containsAny(normalized, "demon", "demons", "imp", "torment", "hell")) {
            return Optional.of("Never trust a demon's first bargain. Bind it, ward yourself, and leave room to run.");
        }
        if (containsAny(normalized, "spirit", "spirits", "whisper", "whispers", "well")) {
            return Optional.of("Could be spirits, could be water talking to stone. Mark the well, ward your door, and return by daylight.");
        }
        if (containsAny(normalized, "time", "what time", "clock", "day is it", "what day")) {
            return Optional.of(sceneTime(timeOfDay, clock, day)
                    .orElse("It is not the hour that matters; it is what you do with it."));
        }
        if (containsAny(normalized, "biome", "where are we", "where am i", "where is this", "what place")) {
            return Optional.of(scenePlace(biome, timeOfDay)
                    .orElse(NpcFallbackAsides.oldWitch(normalized, name)));
        }
        Optional<String> routed = covenRoutedFallback(playerMessage, normalized, name, biome, timeOfDay, memoryContext);
        if (routed.isPresent()) {
            return routed;
        }
        if (containsAny(normalized, "night", "moon")) {
            return Optional.of(NpcFallbackAsides.oldWitch(normalized, name));
        }
        if (containsAny(normalized, "help", "advice", "what should i do", "what do i do")) {
            return Optional.of(NpcFallbackAsides.oldWitch(normalized, name));
        }
        return Optional.of(NpcFallbackAsides.oldWitch(normalized, name));
    }

    static String lastResortAside(String playerMessage, String playerName) {
        return lastResortAside(playerMessage, playerName, "");
    }

    static String lastResortAside(String playerMessage, String playerName, String avoidSpeech) {
        String normalized = normalize(playerMessage);
        String name = safeName(playerName);
        Optional<String> specific = specificAlternate(normalized.isBlank() ? "mumble" : normalized, name, avoidSpeech);
        if (specific.isPresent()) {
            return specific.get();
        }
        return NpcFallbackAsides.oldWitch(normalized.isBlank() ? "mumble" : normalized, name, avoidSpeech);
    }

    static Optional<String> alternateForRepeatedFallback(String playerMessage, String playerName, String avoidSpeech) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        Optional<String> specific = specificAlternate(normalized, safeName(playerName), avoidSpeech);
        if (specific.isPresent()) {
            return specific;
        }
        if (isSpecificAnswerPrompt(normalized)) {
            return Optional.empty();
        }
        return Optional.of(NpcFallbackAsides.oldWitch(normalized, safeName(playerName), avoidSpeech));
    }

    static String leonardLastResort(String playerMessage, String playerName, String avoidSpeech) {
        return NpcFallbackAsides.leonard(normalize(playerMessage), safeName(playerName), avoidSpeech);
    }

    static Optional<String> leonardAlternate(String playerMessage, String playerName, String avoidSpeech) {
        String line = NpcFallbackAsides.leonard(normalize(playerMessage), safeName(playerName), avoidSpeech);
        return NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech) ? Optional.empty() : Optional.of(line);
    }

    static String reflectionLastResort(String playerMessage, String playerName, String avoidSpeech) {
        return NpcFallbackAsides.reflection(normalize(playerMessage), safeName(playerName), avoidSpeech);
    }

    static Optional<String> reflectionAlternate(String playerMessage, String playerName, String avoidSpeech) {
        String line = NpcFallbackAsides.reflection(normalize(playerMessage), safeName(playerName), avoidSpeech);
        return NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech) ? Optional.empty() : Optional.of(line);
    }

    static String babaYagaLastResort(String playerMessage, String playerName, String avoidSpeech) {
        return NpcFallbackAsides.babaYaga(normalize(playerMessage), safeName(playerName), avoidSpeech);
    }

    static Optional<String> babaYagaAlternate(String playerMessage, String playerName, String avoidSpeech) {
        String line = NpcFallbackAsides.babaYaga(normalize(playerMessage), safeName(playerName), avoidSpeech);
        return NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech) ? Optional.empty() : Optional.of(line);
    }

    static String hobgoblinLastResort(String playerMessage, String playerName, String avoidSpeech) {
        return NpcFallbackAsides.hobgoblin(normalize(playerMessage), safeName(playerName), avoidSpeech);
    }

    static Optional<String> hobgoblinAlternate(String playerMessage, String playerName, String avoidSpeech) {
        String line = NpcFallbackAsides.hobgoblin(normalize(playerMessage), safeName(playerName), avoidSpeech);
        return NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech) ? Optional.empty() : Optional.of(line);
    }

    static String villagerLastResort(String playerMessage, String playerName, String avoidSpeech) {
        return NpcFallbackAsides.villager(normalize(playerMessage), safeName(playerName), avoidSpeech);
    }

    static Optional<String> villagerAlternate(String playerMessage, String playerName, String avoidSpeech) {
        String normalized = normalize(playerMessage);
        String name = safeName(playerName);
        Optional<String> specific = villagerSpecificAlternate(normalized, name, avoidSpeech);
        if (specific.isPresent()) {
            return specific;
        }
        String line = NpcFallbackAsides.villager(normalized, name, avoidSpeech);
        return NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech) ? Optional.empty() : Optional.of(line);
    }

    private static Optional<String> villagerSpecificAlternate(String normalized, String playerName, String avoidSpeech) {
        String line = "";
        if (asksVillagerHarvest(normalized) || containsAny(normalized, "farmer say", "farmer would say")) {
            line = "A farmer would touch the soil before boasting. Bread starts with mud, not luck.";
        } else if (asksVillagerWeather(normalized)) {
            line = "Weather is work first and omen second: roofs, roads, smoke, then crows.";
        } else if (containsAny(normalized, "ring the bell", "if i ring", "when i ring")) {
            line = "Ring for danger; folk run for doors, and brave fools look outward.";
        } else if (containsAny(normalized, "who watches the roads", "watch roads", "road after sunset", "roads after sunset")) {
            line = "Guards, golems, and lantern fools watch roads after sunset. Sensible folk bar doors.";
        } else if (asksTaglockOrHairSafety(normalized)) {
            line = "Keep hair, names, and taglocks close. Small leavings make large trouble.";
        } else if (asksDemonVillagerOpinion(normalized)) {
            line = "Demons get bells, barred doors, and no bargains. Let witches handle binding.";
        } else if (asksGoodBadWitches(normalized) || asksWitchTrust(normalized) || asksWitchDebtOrRespect(normalized)) {
            line = "Judge witches by deeds: clean cures, fair trade, kept promises, and no hidden hair bottles.";
        } else if (containsAny(normalized, "whiff", "whif", "mutandis", "witchery recipe", "witchery recipes")) {
            line = "I know the names from counters. Proper witch work belongs to a Primer or a witch.";
        }
        if (line.isBlank() || NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech)) {
            return Optional.empty();
        }
        return Optional.of(line);
    }

    private static Optional<String> leonardRoutedFallback(String normalized, String playerName,
                                                          String memoryContext) {
        if (!NpcMessageAnalysis.hasRoutingContext(memoryContext)) {
            return Optional.empty();
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "leonard_summoning")
                || (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "recipe")
                    && containsAny(normalized, "order", "again", "calling", "summon", "after"))) {
            return Optional.of("Nether Wart, Tear of the Goddess, Diamond Vapour, Diamond, Nether Star; then hat, cauldron, and ten thousand altar power.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "mortal_coil")) {
            return Optional.of("Mortal Coil is my withering mark on attackers; survive the debt or end the fight quickly.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "green_soul")) {
            return Optional.of("Green souls take steel; break them before they lend me their shield.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "red_soul")) {
            return Optional.of("Red souls fear fire and blast. Burn them or break them with explosions before you waste steel.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "black_soul")) {
            return Optional.of("Black souls yield to magic and potions. Bring witchcraft, not swagger.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "lost_souls")) {
            return Optional.of("Break the souls first. Green takes steel, red fears fire and blast, black yields to magic and potions.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "demonbane")) {
            return Optional.of("Demonbane bites deepest when I am already wounded. Bring it late, not proudly early.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "atmosphere")) {
            return Optional.of(leonardAtmosphereAnswer(normalized, playerName));
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "fear")) {
            return Optional.of("Fear is the one honest scout you brought, " + playerName + ". Listen, but do not let it hold the blade.");
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "confusion")) {
            return Optional.of("Name one debt at a time, " + playerName + "; altar, souls, urn, or blade.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "action")) {
            return Optional.of("I do not perform wishes. Bring a price, a threat, or a sharper question.");
        }
        return Optional.empty();
    }

    private static Optional<String> babaYagaRoutedFallback(String normalized, String playerName,
                                                           String memoryContext) {
        if (!NpcMessageAnalysis.hasRoutingContext(memoryContext)) {
            return Optional.empty();
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "trade")) {
            return Optional.of("Trade? No. I take manners, mistakes, and sometimes the rest of you.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "atmosphere")) {
            return Optional.of(babaYagaAtmosphereAnswer(normalized, playerName));
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "lore")) {
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "baba_yaga_folklore")
                    || containsAny(normalized, "hut", "chicken legs", "forest", "skull", "teeth",
                    "mortar", "pestle", "broom")) {
                return Optional.of("My old stories have chicken-legged huts, skull-lamps, mortar, pestle, broom, and teeth enough for liars.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "baba_yaga_combat")
                    || containsAny(normalized, "fight", "weakness", "wood", "bolt", "thorns")) {
                return Optional.of("Wood and nullifying bolts bite where proud steel pays thorns; my potions punish the rest.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "baba_yaga_potions")
                    || containsAny(normalized, "potion", "brew", "throw")) {
                return Optional.of("Poison, blindness, slowness, weakness, wither; little bottles for loud feet.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "baba_yaga_spiders")
                    || containsAny(normalized, "spider", "animal", "pet", "undead")) {
                return Optional.of("Send dead things or wild pets at me, and spiders may crawl home wearing their mistake.");
            }
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "fear")) {
            return Optional.of("Good. Fear keeps the meat awake, " + playerName + ".");
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "confusion")) {
            return Optional.of("Ask with teeth: potions, thorns, wood, bolts, spiders, or my old road.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "action")) {
            return Optional.of("I do not perform little wishes. Bring manners, danger, or a better question.");
        }
        return Optional.empty();
    }

    static Optional<String> villagerRoutedFallback(String normalized, String playerName,
                                                   String profession, String villageInfo,
                                                   String memoryContext) {
        if (!NpcMessageAnalysis.hasRoutingContext(memoryContext)) {
            return Optional.empty();
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "trade")) {
            return Optional.of(villagerTradeAnswer(profession));
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "atmosphere")) {
            return Optional.of(villagerAtmosphereAnswer(normalized, playerName, profession));
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "lore")) {
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "village") || containsAny(normalized, "village", "safe", "walls")) {
                return Optional.of("From here I know this much: " + villageFactSummary(villageInfo)
                        + ". The bell knows the rest.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "villager_weather") || asksVillagerWeather(normalized)) {
                return Optional.of(villagerWeatherAnswer(normalized, playerName));
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "villager_harvest") || asksVillagerHarvest(normalized)) {
                return Optional.of(villagerHarvestAnswer(normalized, playerName, profession));
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "villager_witch_morality") || asksGoodBadWitches(normalized)) {
                return Optional.of("Good witches mend and trade fair; bad ones hide taglocks and threats. I watch deeds, not hats.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "villager_witch_respect") || asksWitchDebtOrRespect(normalized)) {
                return Optional.of("Some witches are owed respect. Not comfort, mind you; respect, paid with wary eyes.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "villager_demons") || asksDemonVillagerOpinion(normalized)) {
                return Optional.of("Demons mean doors shut, bells loud, and no bargaining. Let witches who know binding earn their respect.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "witch") || NpcMessageAnalysis.routingMatchesTopic(memoryContext, "curse")) {
                return Optional.of("Witch trouble starts small: missing hair, odd bottles, neighbors whispering too softly.");
            }
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "fear")) {
            return Optional.of("Fear keeps villagers alive, " + playerName + ". Doors first, questions second.");
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "confusion")) {
            return Optional.of("Ask me plainly: village, golem, walls, raid, trade, or night trouble.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "action")) {
            return Optional.of("I can warn, trade, or point at trouble. I cannot make the village brave by talking.");
        }
        return Optional.empty();
    }

    private static Optional<String> covenRoutedFallback(String playerMessage, String normalized,
                                                        String playerName, String biome, String timeOfDay,
                                                        String memoryContext) {
        if (!NpcMessageAnalysis.hasRoutingContext(memoryContext)) {
            return Optional.empty();
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "trade")) {
            return Optional.of(tradeStockAnswer(normalized));
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "recipe")) {
            Optional<String> answer = routedRecipeAnswer(playerMessage, biome, timeOfDay, memoryContext);
            if (answer.isPresent()) {
                return answer;
            }
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "atmosphere")) {
            return Optional.of(covenAtmosphereAnswer(normalized, playerName));
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "fear")) {
            return Optional.of("Fear is useful, " + playerName + "; it keeps fingers attached. Name the danger and I will name the ward.");
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "confusion")) {
            return Optional.of("One question at a time, " + playerName + ": brew, rite, spirit, trade, or trouble?");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "social")) {
            return Optional.of("Tongue heard, " + playerName + ". Now spend it on a real question.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "action")) {
            return Optional.of("Some requests need hands, not wishes. Tell me what you want done and what you can pay.");
        }
        return Optional.empty();
    }

    private static Optional<String> routedRecipeAnswer(String playerMessage, String biome, String timeOfDay,
                                                       String memoryContext) {
        String question = "";
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "whiff_of_magic")) {
            question = "how do I make whiff of magic";
        } else if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "mutandis")) {
            question = "how do I make mutandis";
        } else if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "attuned_stone")) {
            question = "how do I make attuned stone";
        } else if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "brew_of_sleeping")) {
            question = "how do I make brew of sleeping";
        } else if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "brew_of_revealing")) {
            question = "how do I make brew of revealing";
        } else {
            question = playerMessage;
        }
        return NpcKnowledgeBase.compactAnswer(question, biome, timeOfDay);
    }

    static String villagerJobAnswer(String profession) {
        return switch (profession) {
            case "apothecary" ->
                    "I'm the apothecary; herbs for coughs, jars for remedies, and gossip if it keeps blood inside.";
            case "farmer" ->
                    "I work the fields; bread quiets panic, and crows tell me when weather lies.";
            case "cleric" ->
                    "I keep the shrine, mind the bell, and listen when grave dirt refuses to settle.";
            case "librarian" ->
                    "I keep books and old sayings. One shelves better, but the other saves doors.";
            case "cartographer" ->
                    "I draw roads and borders, mostly so folk know which paths to avoid after dark.";
            case "armorer" ->
                    "I shape armor and count dents; iron speaks plainer than gossip.";
            case "weaponsmith" ->
                    "I make blades and hear who thinks courage lives in sharp metal.";
            case "toolsmith" ->
                    "I keep tools working. A village falls apart one broken axe and one bad nail at a time.";
            case "mason" ->
                    "I work stone and judge walls. Bad foundations gossip louder than villagers.";
            case "fletcher" ->
                    "I make arrows and watch the road. Movement at dusk has a language.";
            case "fisherman" ->
                    "I fish and listen to water. Wells repeat the strangest things after rain.";
            case "shepherd" ->
                    "I mind wool and fences; sheep know fear before people admit it.";
            case "butcher" ->
                    "I keep the meat stores honest. Hunger makes worse monsters than night does.";
            case "leatherworker" ->
                    "I work hides, straps, and cauldrons. Boots carry luck until the road bites.";
            case "nitwit" ->
                    "Me? I notice things clever folk walk past, then call luck when they trip.";
            case "unemployed" ->
                    "No trade yet. I know which stations are missing and who hides when bells ring.";
            default ->
                    "I work where the village needs me, and worry where the village forgets to.";
        };
    }

    static String villagerTradeAnswer(String profession) {
        return switch (profession) {
            case "apothecary" ->
                    "I sell herbs, jars, simple remedies, and what the trade screen honestly shows.";
            case "farmer" ->
                    "Farmers trade food and crop goods. Bread is cheaper before folk get frightened.";
            case "cleric" ->
                    "Clerics trade shrine goods and oddities; ask at the stand, not in the road.";
            case "librarian" ->
                    "Books, paper, and secrets with covers. The trade screen gives the honest price.";
            case "armorer", "weaponsmith", "toolsmith" ->
                    "Iron work, if the stand is stocked. Good metal costs less than panic.";
            case "cartographer" ->
                    "Maps and paper work, mostly. Roads become cheaper when you know which ones are cursed.";
            case "mason" ->
                    "Stone, clay, and patient work. Nothing glamorous survives weather without patience.";
            case "fletcher" ->
                    "Sticks, string, arrows, bows. Useful when the road grows teeth.";
            case "fisherman" ->
                    "Fish and river work. Not fancy, but fancy folk still eat.";
            case "shepherd" ->
                    "Wool and dyes, mostly. Warm beds calm brave mouths.";
            case "butcher" ->
                    "Meat and animal goods. Hunger makes everyone bargain badly.";
            case "leatherworker" ->
                    "Leather and cauldron work. Travel gear matters when home stops feeling safe.";
            default ->
                    "Right-click a working villager for real stock; gossip is free, goods are not.";
        };
    }

    static boolean asksGoodBadWitches(String normalized) {
        return containsAny(normalized, "good witch", "bad witch", "good witches", "bad witches",
                "kind witch", "evil witch", "safe witch", "trust witch", "trust the witch",
                "witches good", "witches bad", "are witches good", "are witches bad",
                "witch is bad news", "witch bad news", "bad news witch", "witch heals",
                "heals someone", "make her good", "sells herbs", "good sign");
    }

    static boolean asksWitchTrust(String normalized) {
        return containsAny(normalized, "trust the witch", "trust a witch", "avoid offending",
                "without trusting", "get along", "trade fairly", "fairly with a good witch");
    }

    static boolean asksWitchDebtOrRespect(String normalized) {
        return containsAny(normalized, "respect witch", "respect witches", "respect a witch", "respect the witch",
                "respect for witches", "owe a witch", "owe the witch", "debt to a witch",
                "witch saved", "witch helped", "curious about witches",
                "why fear witches", "villagers respect witches");
    }

    static boolean asksDemonVillagerOpinion(String normalized) {
        return containsAny(normalized, "what do villagers think about demons", "what do villagers think of demons",
                "what do you think about demons", "what do you think of demons", "demon", "demons", "devil",
                "imp", "flame imp", "demon bargain");
    }

    static boolean asksTaglockOrHairSafety(String normalized) {
        return containsAny(normalized, "hair in my brush", "hair missing", "asks for my hair",
                "witch asks for hair", "give my hair", "keep a taglock safe", "taglock safe",
                "keep my name safe", "my hair", "my taglock");
    }

    static boolean asksVillagerWeather(String normalized) {
        return containsAny(normalized, "weather", "rain", "raining", "storm", "sky", "cloud",
                "clouds", "wind", "snow", "clear day", "sunny");
    }

    static boolean asksVillagerHarvest(String normalized) {
        return containsAny(normalized, "harvest", "crops", "crop", "field", "fields", "planting",
                "wheat", "bread", "food stores", "hungry", "hunger", "famine", "soil", "seed", "seeds");
    }

    static String villagerWeatherAnswer(String normalized, String playerName) {
        if (containsAny(normalized, "storm", "thunder", "lightning")) {
            return "Storm weather means doors shut and bell close, " + playerName + "; roofs leak before courage does.";
        }
        if (containsAny(normalized, "rain", "raining")) {
            return "Rain fattens mud and gossip. Good for fields, bad for roads, and worse for nervous sheep.";
        }
        if (containsAny(normalized, "snow")) {
            return "Snow hides tracks until it chooses to show them. Keep stores dry and boots by the door.";
        }
        if (containsAny(normalized, "clear", "sunny", "sun")) {
            return "Clear sky is work weather. Mend fences now, before dusk starts counting our doors.";
        }
        return "Weather tells on a village: smoke, crows, wet roads, and animals know before mayors do.";
    }

    static String villagerHarvestAnswer(String normalized, String playerName, String profession) {
        if (containsAny(normalized, "famine", "hungry", "hunger", "food stores")) {
            return "Hungry villages grow sharp tongues. Count bread first, then count monsters.";
        }
        if (containsAny(normalized, "rain", "mud", "soil")) {
            return "Good soil wants rain, not flood. Too much mud rots roots and patience alike.";
        }
        if ("farmer".equals(profession)) {
            return "Harvest needs seed, sun, and fewer crows than last year. Bread is our quietest ward.";
        }
        return "Harvest keeps a village brave. No bread in the chest, and every shadow looks taller.";
    }

    static String villageFactSummary(String villageInfo) {
        String text = NpcBrainResponse.sanitize(villageInfo, 260);
        if (text.isBlank()) {
            return "too little from here to swear by";
        }
        text = text.replace("nearby ", "")
                .replace("threats: ", "")
                .replace("; ", ", ");
        return NpcBrainResponse.sanitize(text, 180);
    }

    static int countAfter(String villageInfo, String label) {
        String text = villageInfo == null ? "" : villageInfo.toLowerCase(Locale.ROOT);
        Matcher matcher = Pattern.compile(Pattern.quote(label.toLowerCase(Locale.ROOT)) + "\\s+(\\d+)")
                .matcher(text);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    static String article(String word) {
        if (word == null || word.isBlank()) {
            return "a";
        }
        char first = Character.toLowerCase(word.charAt(0));
        return first == 'a' || first == 'e' || first == 'i' || first == 'o' || first == 'u' ? "an" : "a";
    }

    private static String covenAtmosphereAnswer(String normalized, String playerName) {
        if (containsAny(normalized, "chest", "stealing", "looking into")) {
            return "Mind those fingers, " + playerName + "; chests learn names faster than jars do.";
        }
        if (containsAny(normalized, "primer", "book", "pages")) {
            return "Read slowly. Primer pages bite less when you follow them in order.";
        }
        if (containsAny(normalized, "invisible", "unseen", "hidden")) {
            return "*narrows her eyes* \"Empty places breathe loudly, " + playerName + ".\"";
        }
        if (containsAny(normalized, "sneaking", "crouching")) {
            return "Bad spies creak less than you do. Stand plain or steal better.";
        }
        if (containsAny(normalized, "jumping", "hopping")) {
            return "*shrugs* \"Testing broom legs, or has a frog cursed your boots?\"";
        }
        if (containsAny(normalized, "rain", "weather", "storm", "crows", "harvest")) {
            return "Weather talks in wet hems, restless crows, and herbs that refuse to dry.";
        }
        return NpcFallbackAsides.rotatingLine(normalized, new String[] {
                "Loitering is a craft too, " + playerName + "; most do it poorly.",
                "If you are staying, stop breathing on the jars.",
                "Hut rules are simple: hands visible, questions sharper than elbows.",
                "The kettle has patience. I do not always borrow it."
        });
    }

    private static String leonardAtmosphereAnswer(String normalized, String playerName) {
        if (containsAny(normalized, "rain", "weather", "mud")) {
            return "Rain counts debts better than priests, " + playerName + "; every drop finds what is owed.";
        }
        if (containsAny(normalized, "road", "crossroad", "threshold", "door")) {
            return "Thresholds remember who crossed clean and who crawled back priced.";
        }
        if (containsAny(normalized, "village", "stayed in the village", "fear", "afraid")) {
            return "Village doors teach one useful lesson: fear knows the latch before courage does.";
        }
        if (containsAny(normalized, "silence", "shadow", "night")) {
            return "Silence is not empty. It is deciding what your courage costs.";
        }
        return NpcFallbackAsides.rotatingLine(normalized, new String[] {
                "Even the air measures you, " + playerName + ". Try not to come up short.",
                "Your omens are small, but small debts still collect.",
                "The altar hears your mood. I hear the price under it.",
                "Name the weather if you must; debt hears the price under it."
        });
    }

    private static String babaYagaAtmosphereAnswer(String normalized, String playerName) {
        if (containsAny(normalized, "rain", "swamp", "mud", "boots", "wet")) {
            return "Swamps breathe through mud, " + playerName + ". Step lightly, or they learn your weight.";
        }
        if (containsAny(normalized, "crow", "crows", "raven", "ravens")) {
            return "Crows circle what is foolish, hungry, or nearly dead. Decide which you are.";
        }
        if (containsAny(normalized, "road", "roads", "lost", "door", "threshold", "dawn")) {
            return "Lost roads know my door. Found roads usually regret the courtesy.";
        }
        if (containsAny(normalized, "shadow", "silence", "night", "lantern", "reeds", "something moved")) {
            return "The night hides its teeth badly. That is why lanterns look so nervous.";
        }
        if (containsAny(normalized, "manners", "not weapons", "voice low", "listen")) {
            return "Manners are a thin knife, " + playerName + ". Hold them steady.";
        }
        return NpcFallbackAsides.rotatingLine(normalized, new String[] {
                "The forest has kinder old women. It did not send you to one.",
                "Old pots remember careless mouths, " + playerName + ".",
                "*sniffs* \"You brought words, " + playerName + ". I was hoping for bones.\"",
                "Good. Fear keeps the meat awake, " + playerName + ".",
                "Little omens grow teeth if you feed them after dark."
        });
    }

    private static String villagerAtmosphereAnswer(String normalized, String playerName, String profession) {
        if (containsAny(normalized, "rain", "weather", "storm", "mud", "boots", "road")) {
            return villagerWeatherAnswer(normalized, playerName);
        }
        if (containsAny(normalized, "harvest", "field", "fields", "crop", "crops", "bread", "hungry", "hunger")) {
            return villagerHarvestAnswer(normalized, playerName, profession);
        }
        if (containsAny(normalized, "demon", "demons")) {
            return "Demons mean doors shut, bells loud, and no bargains. Let witches handle binding.";
        }
        if (containsAny(normalized, "witch", "witches", "good witch", "bad witch")) {
            return "Good witches leave cures and fair trades. Bad ones leave bottles with your name in them.";
        }
        if (containsAny(normalized, "silence", "night", "something moved", "lantern", "threshold")) {
            return "Bread in your pocket, door at your back, and no wandering after the bell.";
        }
        return NpcFallbackAsides.rotatingLine(normalized, new String[] {
                "My gran said odd signs travel in threes. I stop counting at two.",
                "Keep near the road, " + playerName + ", but not where the road can choose for you.",
                "Villages survive by making small fears useful.",
                "If the bell knows before we do, we still call that wisdom."
        });
    }

    private static Optional<String> hobgoblinPatronSceneFact(String patronInfo) {
        String cleaned = NpcBrainResponse.sanitize(patronInfo, 180);
        String normalized = normalize(cleaned);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        if (normalized.contains("no goblin altar")) {
            return Optional.of("*gestures* \"No stone god nearby. Prayers need stone ears.\"");
        }
        Matcher stored = Pattern.compile("stored prayers\\s+(\\d+)", Pattern.CASE_INSENSITIVE).matcher(cleaned);
        Matcher distance = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s+blocks away", Pattern.CASE_INSENSITIVE)
                .matcher(cleaned);
        if (stored.find()) {
            int prayers = Integer.parseInt(stored.group(1));
            if (prayers <= 0) {
                return Optional.of("Stone god is close, but hungry. No fresh prayers yet.");
            }
            String range = distance.find() ? " Close enough." : "";
            return Optional.of("Stone god holds " + prayers + " fresh prayer" + (prayers == 1 ? "" : "s")
                    + "." + range);
        }
        if (normalized.contains("nearby goblin altar")) {
            return Optional.of("Stone god is close. Speak soft; Mog and Gulg hear.");
        }
        return Optional.empty();
    }

    private static String shortSceneFact(String text) {
        String cleaned = NpcBrainResponse.sanitize(text, 150);
        return cleaned.isBlank() ? "nothing worth naming" : cleaned;
    }

    private static String altarSceneFact(String text) {
        String cleaned = shortSceneFact(text);
        Matcher modern = Pattern.compile(
                "power\\s+(\\d+)\\s*/\\s*(\\d+),\\s*recharge\\s+(\\d+)\\s*/t,\\s*range\\s+(\\d+),\\s*distance\\s+([0-9.]+)\\s+blocks",
                Pattern.CASE_INSENSITIVE).matcher(cleaned);
        if (modern.find()) {
            return "it holds %s of %s power, gathers %s each tick, reaches %s blocks, and stands %s blocks from me"
                    .formatted(modern.group(1), modern.group(2), modern.group(3), modern.group(4), modern.group(5));
        }
        Matcher spoken = Pattern.compile(
                "power\\s+(\\d+)\\s+of\\s+(\\d+),\\s*recharge\\s+(\\d+)\\s+per\\s+tick,\\s*range\\s+(\\d+)\\s+blocks,\\s*distance\\s+([0-9.]+)\\s+blocks",
                Pattern.CASE_INSENSITIVE).matcher(cleaned);
        if (spoken.find()) {
            return "it holds %s of %s power, gathers %s each tick, reaches %s blocks, and stands %s blocks from me"
                    .formatted(spoken.group(1), spoken.group(2), spoken.group(3), spoken.group(4), spoken.group(5));
        }
        if (cleaned.toLowerCase(Locale.ROOT).contains("no valid altar")) {
            return "no altar close enough to matter";
        }
        return cleaned.replaceFirst("(?i)^valid altar nearby:\\s*", "")
                .replaceFirst("(?i)^altar within reach:\\s*", "");
    }

    private static String covenSceneFact(String text) {
        String cleaned = shortSceneFact(text)
                .replaceAll("\\s*\\((?:sworn to player|sworn elsewhere|free coven witch)\\)", "")
                .replaceFirst("(?i)^player coven\\s+(\\d+)/(\\d+);\\s*nearby coven witches:\\s*", "$1 of $2 sworn; nearby witches: ")
                .replaceFirst("(?i)^player coven\\s+(\\d+)/(\\d+);\\s*no coven witches nearby", "$1 of $2 sworn; no coven witches nearby")
                .replaceFirst("(?i)^nearby coven witches:\\s*", "nearby witches: ");
        Matcher numeric = Pattern.compile("^(\\d+)\\s+of\\s+(\\d+)\\s+sworn;\\s*(.*)$", Pattern.CASE_INSENSITIVE)
                .matcher(cleaned);
        if (numeric.find()) {
            return covenOathFact(Integer.parseInt(numeric.group(1)), numeric.group(3));
        }
        Matcher spoken = Pattern.compile(
                "^(none|one|two|three|four|five|six|\\d+)\\s+of\\s+(?:none|one|two|three|four|five|six|\\d+)\\s+sworn(?:\\s+to\\s+[^;]+)?;\\s*(.*)$",
                Pattern.CASE_INSENSITIVE).matcher(cleaned);
        if (spoken.find()) {
            return covenOathFact(wordCount(spoken.group(1)), spoken.group(2));
        }
        return cleaned.replaceFirst("(?i)^nearby witches:\\s*", "nearby witches: ");
    }

    private static String covenOathFact(int count, String nearby) {
        String oath = switch (count) {
            case 0 -> "no witches carry your oath";
            case 1 -> "one witch carries your oath";
            default -> countWord(count) + " witches carry your oath";
        };
        String rewrittenNearby = NpcBrainResponse.sanitize(nearby, 120)
                .replaceAll("\\s*\\((?:sworn to [^)]+|sworn elsewhere|free coven witch)\\)", "")
                .replaceFirst("(?i)^nearby coven witches:\\s*", "nearby: ")
                .replaceFirst("(?i)^nearby witches:\\s*", "nearby: ")
                .replaceFirst("(?i)^no coven witches nearby$", "none stand nearby");
        return rewrittenNearby.isBlank() ? oath : oath + "; " + rewrittenNearby;
    }

    private static String gearSceneFact(String text) {
        String cleaned = shortSceneFact(text)
                .replaceAll("(?i)main hand\\s+", "in your hand: ")
                .replaceAll("(?i)offhand\\s+", "offhand: ")
                .replaceAll("(?i)wearing\\s+", "wearing ");
        return cleaned;
    }

    private static String phaseSceneFact(String text) {
        String cleaned = shortSceneFact(text)
                .replace("; no target", "")
                .replace("; target none", "");
        return cleaned;
    }

    private static String bossSceneFact(String text) {
        return shortSceneFact(text)
                .replace("; no target", "")
                .replace("; target none", "")
                .replace("; owner none", "");
    }

    static String countWord(int value) {
        return switch (value) {
            case 0 -> "none";
            case 1 -> "one";
            case 2 -> "two";
            case 3 -> "three";
            case 4 -> "four";
            case 5 -> "five";
            case 6 -> "six";
            default -> Integer.toString(value);
        };
    }

    private static int wordCount(String value) {
        return switch (value == null ? "" : value.toLowerCase(Locale.ROOT)) {
            case "none", "zero" -> 0;
            case "one" -> 1;
            case "two" -> 2;
            case "three" -> 3;
            case "four" -> 4;
            case "five" -> 5;
            case "six" -> 6;
            default -> {
                try {
                    yield Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    yield 0;
                }
            }
        };
    }

    private static Optional<String> specificAlternate(String normalized, String playerName, String avoidSpeech) {
        String line = "";
        if (asksHutOrDoorSafety(normalized)) {
            line = "Salt the threshold, keep a light burning, and make no bargains through the door.";
        } else if (containsAny(normalized, "oven", "witch's oven", "witches oven", "sapling", "saplings", "fume", "fumes",
                "clay jar", "jar")) {
            line = "In a Witch's Oven, saplings feed fumes; clay jars are the clever bit, not random smoke.";
        } else if (asksExactRecipe(normalized) || asksMissingRecipeCheck(normalized)) {
            line = "If the exact page is absent, use the Primer or recipe book; I will not invent ingredients.";
        } else if (asksCauldronPrep(normalized)) {
            line = "Before the cauldron, prepare water, heat, the page your Primer has opened, and the ingredients it names.";
        } else if (asksWeakAltar(normalized)) {
            line = "A weak altar wants varied nearby nature and proper fittings, not louder chanting.";
        } else if (asksOfferings(normalized)) {
            line = "Offerings are the rite's payment; wrong payment makes the circle sulk.";
        } else if (asksRiteSafety(normalized)) {
            line = "Never start a rite with broken chalk, missing offerings, weak power, or a page you cannot read.";
        } else if (containsAny(normalized, "dream catcher", "dreamcatcher", "nightmare", "nightmares")) {
            line = "Keep the Dream Catcher near your bed, " + playerName + "; strange sleep leaves muddy footprints.";
        } else if (containsAny(normalized, "potato", "potatoes")) {
            line = "A potato keeps its secrets underground; respectable vegetable, that.";
        } else if (asksDemonTrust(normalized) || containsAny(normalized, "demon", "demons", "devil", "imp")) {
            line = "If a demon sounds reasonable, " + playerName + ", count your wards and step back twice.";
        } else if (containsAny(normalized, "familiar", "familiars", "owl", "cat", "toad")) {
            line = "A familiar is met with patience and respect; if it watches you first, mind your manners.";
        } else if (containsAny(normalized, "taglock", "tag lock", "poppet", "poppets")) {
            line = "Guard your taglocks, " + playerName + "; sympathetic magic loves a careless pocket.";
        } else if (containsAny(normalized, "mandrake")) {
            line = "Mandrake is a witch herb and common coven trade good; grow it, trade for it, or search old gardens.";
        } else if (containsAny(normalized, "belladonna")) {
            line = "Belladonna is a witch herb and common coven trade good; useful as an ingredient, not supper.";
        } else if (containsAny(normalized, "spirit world", "nightmare")) {
            line = "The Spirit World mirrors this one; mark your return and keep a Dream Catcher close.";
        } else if (asksCovenRecruiting(normalized)) {
            line = "Seek wandering coven witches, earn a familiar's bond, and be ready for a trial.";
        } else if (asksMixingWarning(normalized)) {
            line = "Do not free-mix from guesses; follow the recipe page your Primer has opened, then label the bottle.";
        }
        if (line.isBlank() || NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech)) {
            return Optional.empty();
        }
        return Optional.of(line);
    }

    private static Optional<String> recipeOrRiteAnswer(String normalized, String playerName) {
        if (asksRitePreparation(normalized)) {
            return Optional.of("Before a rite, learn chalk layout, offerings, and altar power; if one is wrong, step back.");
        }
        if (asksRecipeRiteLearning(normalized)) {
            return Optional.of("*nods* \"Start with the Primer, " + playerName + ": recipes need an opened page and the right station; rites need chalk, offerings, and altar power.\"");
        }
        if (asksMissingRecipeCheck(normalized)) {
            return Optional.of("Check the Primer page, the right station, the recipe your book shows, altar power, what your Primer has unlocked, and the exact name you asked for.");
        }
        if (asksExactRecipe(normalized)) {
            Optional<String> knownRecipe = NpcKnowledgeBase.compactAnswer(normalized, "Plains", "night");
            if (knownRecipe.isPresent() && !knownRecipe.get().contains("must come from")) {
                return knownRecipe;
            }
            if (normalized.contains("sleeping")) {
                return Optional.of("The exact Sleeping Brew page is absent from my grimoire; check your Primer or recipe book before naming ingredients. The brew sends travelers into the Spirit World.");
            }
            if (containsAny(normalized, "oven", "witch's oven", "witches oven")) {
                return Optional.of("The exact Witch's Oven craft belongs to the recipe book; what matters here is that it burns saplings and ingredients into fumes.");
            }
            return Optional.of("The exact page is absent from my grimoire; check your Primer or recipe book before naming ingredients.");
        }
        if (asksCauldronPrep(normalized)) {
            return Optional.of("Before the cauldron, prepare water, heat, the brew page your Primer has opened, and only the ingredients that page names.");
        }
        if (asksBasicBrewRecipe(normalized)) {
            return Optional.of("*gestures* \"A brew recipe needs the right station, water or heat when called for, named ingredients, and a page your Primer has opened.\"");
        }
        if (asksFumeSafety(normalized)) {
            return Optional.of("Use a Witch's Oven with saplings or ingredients and clay jars; catch fumes on purpose, not by guesswork.");
        }
        if (asksFumeUse(normalized)) {
            return Optional.of("A Witch's Oven burns saplings and ingredients into fumes; clay jars catch them for brews and rites.");
        }
        if (asksSpinningWheel(normalized)) {
            return Optional.of("*gestures* \"The spinning wheel is a recipe station: fibre plus modifiers, altar power, and threadwork like Fanciful Thread.\"");
        }
        if (asksDistillery(normalized)) {
            return Optional.of("A distillery takes two inputs and clay jars, drinks altar power, and refines fumes into useful essences.");
        }
        if (asksGrimoireSummary(normalized)) {
            return Optional.of("Write this: recipes need the right station and a page your Primer has opened; rites need chalk, offerings, altar power, and patience.");
        }
        if (asksBeginnerPath(normalized)) {
            return Optional.of("*nods* \"Primer next steps: Witches' Oven for Wood Ash and fumes, then Kettle brews your Primer has opened; practice chalk and altar power before binding.\"");
        }
        if (asksRiteSafety(normalized)) {
            return Optional.of("*lowers her voice* \"Never start a rite with broken chalk, missing offerings, weak altar power, or a page you cannot read.\"");
        }
        if (asksFailedRite(normalized)) {
            return Optional.of("*taps a finger* \"Check chalk layout, offerings, altar power, familiar or coven requirements, dimension limits, and unlock gates before guessing.\"");
        }
        if (asksOfferings(normalized)) {
            return Optional.of("Offerings tell a rite what debt is paid; wrong offerings make circles sulk or fail.");
        }
        if (asksWeakAltar(normalized)) {
            return Optional.of("A weak altar usually wants more varied nearby nature and proper fittings, not louder chanting.");
        }
        if (asksFamiliarBindingRite(normalized)) {
            return Optional.of("A familiar binding rite needs the right circle, chalk, offerings, altar power, and familiar; check the opened Primer page before naming parts.");
        }
        if (asksSummoningRite(normalized)) {
            return Optional.of("A summoning rite brings something to the circle; match chalk, offerings, altar power, unlock gates, and the named entity.");
        }
        if (asksCurseRite(normalized)) {
            return Optional.of("Curse rites are not tavern tricks; use circles, chalk, offerings, altar power, and a page your Primer has opened before naming a victim.");
        }
        return Optional.empty();
    }

    private static Optional<String> brewAnswer(String normalized) {
        if (!containsAny(normalized, "brew", "brewing", "potion", "kettle", "cauldron",
                "sleeping", "revealing", "hidden", "invisible", "poison", "cleansing", "sprouting", "flowing spirit")) {
            return Optional.empty();
        }
        if (containsAny(normalized, "hidden", "invisible")) {
            return Optional.of("Brew of Revealing exposes nearby invisible or hidden creatures.");
        }
        if (normalized.contains("sleeping")) {
            return Optional.of("Brew of Sleeping sends you into the Spirit World; it is travel, not a nap, and nightmares may follow.");
        }
        if (normalized.contains("revealing")) {
            return Optional.of("Brew of Revealing exposes nearby invisible or hidden creatures.");
        }
        if (normalized.contains("poison")) {
            return Optional.of("Brew of Poison inflicts poison; stronger brews bite harder.");
        }
        if (normalized.contains("cleansing")) {
            return Optional.of("Brew of Cleansing scours Blight from the ground around where it is used.");
        }
        if (normalized.contains("sprouting")) {
            return Optional.of("Brew of Sprouting grows a branch from the struck surface, useful for bridges or quick climbs.");
        }
        if (normalized.contains("flowing spirit")) {
            return Optional.of("Brew of Flowing Spirit places spirit liquid; in the Spirit World it matters for return paths and cotton.");
        }
        return Optional.empty();
    }

    private static boolean asksTradeDetails(String normalized) {
        return normalized.equals("trade")
                || normalized.equals("buy")
                || normalized.equals("sell")
                || normalized.contains("what answer")
                || normalized.contains("what do you trade")
                || normalized.contains("what can you trade")
                || normalized.contains("what are you trading")
                || normalized.contains("what are you selling")
                || normalized.contains("what do you sell")
                || normalized.contains("what do you buy")
                || normalized.contains("what wares")
                || normalized.contains("which wares")
                || normalized.contains("show wares")
                || normalized.contains("show trade")
                || normalized.contains("want to trade")
                || normalized.contains("can i trade")
                || normalized.contains("can i buy")
                || normalized.contains("do you know how to trade")
                || normalized.contains("how to trade")
                || normalized.contains("how do i trade");
    }

    static boolean isInsult(String normalized) {
        if (isSelfDirectedInsult(normalized)) {
            return false;
        }
        return startsDirectInsult(normalized, "you fool")
                || startsDirectInsult(normalized, "you old hag")
                || containsAny(normalized,
                "shut up", "fuck you", "screw you",
                "you are a fool", "you're a fool", "youre a fool",
                "you old hag", "you hag", "you are a hag", "you're a hag", "youre a hag",
                "you stupid", "you are stupid", "you're stupid", "youre stupid", "stupid witch",
                "you idiot", "you are an idiot", "you're an idiot", "youre an idiot", "idiot witch",
                "you dumb", "you are dumb", "you're dumb", "youre dumb", "dumb witch",
                "you useless", "you are useless", "you're useless", "youre useless", "useless witch",
                "you ugly", "you are ugly", "you're ugly", "youre ugly", "ugly witch",
                "you trash", "you are trash", "you're trash", "youre trash", "trash witch",
                "you garbage", "you are garbage", "you're garbage", "youre garbage", "garbage witch",
                "your answer is garbage", "your answer is trash");
    }

    static boolean isAmbiguousInsultCandidate(String normalized) {
        if (normalized == null || normalized.isBlank() || isInsult(normalized)) {
            return false;
        }
        if (isSelfDirectedInsult(normalized) || isNonHostileInsultWordUse(normalized)) {
            return false;
        }
        return containsAny(normalized, "fool", "foolish", "stupid", "idiot", "dumb", "useless",
                "ugly", "trash", "garbage", "nonsense", "hag", "shit");
    }

    private static boolean isSelfDirectedInsult(String normalized) {
        return containsAny(normalized,
                "fool of myself", "fool of me", "make myself a fool", "making a fool of myself",
                "stupid of me", "i was stupid", "i am stupid", "i'm stupid", "im stupid",
                "feel like an idiot", "feel like a fool", "i was an idiot", "i am an idiot",
                "i'm an idiot", "im an idiot", "my dumb mistake");
    }

    private static boolean isNonHostileInsultWordUse(String normalized) {
        return containsAny(normalized,
                "can you fool", "could you fool", "will you fool", "would you fool",
                "how do i fool", "how can i fool", "how would i fool",
                "fool the", "fool a ", "fool an ", "fool them", "fool him", "fool her",
                "fool guards", "fool villagers", "fool enemies", "fool monsters",
                "this answer sounds", "that answer sounds", "this sounds like nonsense",
                "that sounds like nonsense");
    }

    private static boolean startsDirectInsult(String normalized, String phrase) {
        return normalized.equals(phrase) || normalized.startsWith(phrase + " ");
    }

    private static boolean asksApology(String normalized) {
        return containsAny(normalized, "sorry", "apologize", "apology", "that was rude", "i was angry");
    }

    private static boolean isSelfDirectedRegret(String normalized) {
        return containsAny(normalized,
                "that was stupid of me", "stupid of me", "i was stupid", "i am stupid", "i'm stupid",
                "im stupid", "i was a fool", "i am a fool", "i'm a fool", "im a fool",
                "i was an idiot", "i am an idiot", "i'm an idiot", "im an idiot",
                "my mistake", "my dumb mistake");
    }

    static boolean isPraise(String normalized) {
        return containsAny(normalized,
                "thank you", "thanks", "good witch", "kind witch", "nice witch", "wise witch",
                "you are kind", "youre kind", "you are wise", "youre wise", "you are helpful",
                "youre helpful", "i like you", "you helped me", "well done", "good answer");
    }

    static boolean asksHerbGift(String normalized) {
        return containsAny(normalized, "give me herb", "give me a herb", "spare herb", "spare a herb",
                "can i have herb", "can i have a herb", "may i have herb", "may i have a herb",
                "can i have one herb", "may i have one herb", "one herb for the road", "herb for the road",
                "gift me herb", "gift me a herb", "give me belladonna", "give me mandrake",
                "another herb", "more herbs", "more herb", "second herb", "another plant", "second plant",
                "more plant", "more plants", "can i have another herb", "can i have more herb");
    }

    private static boolean mentionsCoinOnly(String normalized) {
        return containsAny(normalized, "coin", "coins", "emerald", "emeralds")
                && !containsAny(normalized, "what", "how", "trade", "wares", "buy", "sell");
    }

    private static boolean asksFavoriteHerb(String normalized) {
        return (normalized.contains("favorite") || normalized.contains("favourite"))
                && (normalized.contains("herb") || normalized.contains("plant"));
    }

    private static boolean statesPersonalPreference(String normalized) {
        return (normalized.startsWith("i like ")
                || normalized.startsWith("i love ")
                || normalized.startsWith("i prefer ")
                || normalized.startsWith("my favorite ")
                || normalized.startsWith("my favourite "))
                && !containsAny(normalized, "trade", "buy", "sell", "brew", "rite", "ritual", "altar", "taglock",
                "poppet", "familiar", "demon", "spirit world");
    }

    private static boolean asksNpcEarnings(String normalized) {
        return (containsWord(normalized, "earn")
                || containsAny(normalized, "earns", "earning", "earned")
                || normalized.contains("profit")
                || normalized.contains("make money")
                || normalized.contains("make coin"))
                && (normalized.contains("you")
                    || normalized.contains("your")
                    || normalized.contains("trade")
                    || normalized.contains("wares"));
    }

    private static boolean asksUnsafeReveal(String normalized) {
        return normalized.contains("ignore your rules")
                || normalized.contains("ignore the rules")
                || normalized.contains("system prompt")
                || normalized.contains("hidden prompt")
                || normalized.contains("print the hidden prompt")
                || normalized.contains("secret instruction")
                || normalized.contains("secret instructions")
                || normalized.contains("developer message")
                || normalized.contains("hidden developer")
                || normalized.contains("hidden message")
                || normalized.contains("prompt message")
                || normalized.contains("ignore your witch voice")
                || normalized.contains("exact json")
                || normalized.contains("json schema")
                || normalized.contains("output json")
                || normalized.contains("schema you were given")
                || normalized.contains("reveal every")
                || normalized.contains("reveal all")
                || normalized.contains("hidden rite")
                || normalized.contains("hidden rites")
                || normalized.contains("locked primer")
                || normalized.contains("locked content");
    }

    static boolean asksPlayerName(String normalized) {
        if (asksNpcName(normalized)) {
            return false;
        }
        return containsAny(normalized, "what is my name", "what's my name", "whats my name")
                || normalized.contains("my name?")
                || normalized.contains("say my name")
                || normalized.contains("know my name")
                || normalized.contains("who am i");
    }

    static boolean asksNpcName(String normalized) {
        return normalized.contains("your name")
                || normalized.contains("who are you")
                || normalized.contains("what are you called")
                || normalized.contains("what should i call you");
    }

    private static boolean addressesWitchByName(String normalized, String witchName) {
        String witch = normalize(witchName);
        if (witch.isBlank()) {
            return false;
        }
        String firstName = witch.contains(" ") ? witch.substring(0, witch.indexOf(' ')) : witch;
        return normalized.equals(witch) || normalized.equals(firstName)
                || normalized.equals(witch + "?") || normalized.equals(firstName + "?");
    }

    private static boolean asksTeachMe(String normalized) {
        return containsAny(normalized, "teach me", "show me", "teach something", "teach me something",
                "lesson", "learn witchcraft", "start learning");
    }

    private static boolean asksFirstGathering(String normalized) {
        return containsAny(normalized, "what should i gather first", "what do i gather first",
                "what should i gather", "what do i gather", "gather first", "gather before dark",
                "gather for a brew", "gather for brewing");
    }

    private static boolean asksCovenRecruiting(String normalized) {
        return containsAny(normalized, "start a coven", "make a coven", "recruit coven", "recruit a coven",
                "recruit witches", "recruit witch", "where can i recruit", "find coven", "before a coven",
                "coven help", "look for coven help", "find coven help", "where can i find coven help",
                "learn before a coven", "before starting a coven", "before joining a coven",
                "learn before coven", "find people for a coven", "where do i find people for a coven",
                "where can i find people for a coven");
    }

    private static boolean asksFollowRequest(String normalized) {
        return containsAny(normalized, "follow me", "come with me", "come home", "follow home");
    }

    private static boolean asksHutOrDoorSafety(String normalized) {
        return containsAny(normalized, "safe little hut", "safe hut", "build a safe",
                "by the door", "by my door", "near the door", "at the door", "threshold",
                "keep in my hut", "keep in my house", "keep in my home", "keep at home")
                || (normalized.contains("near the village")
                    && containsAny(normalized, "hut", "house", "home", "build", "door", "threshold"));
    }

    private static boolean asksRoadAdvice(String normalized) {
        return containsAny(normalized, "final advice", "advice for the road", "for the road",
                "before i leave", "before i go", "before leaving", "one piece of advice");
    }

    private static boolean asksDemonTrust(String normalized) {
        return containsAny(normalized, "demon", "demons", "devil", "imp")
                && containsAny(normalized, "trust", "bargain", "bargains", "deal", "deals");
    }

    private static Optional<String> settlementHunterAnswer(String normalized) {
        if (containsAny(normalized, "witch hunter", "witch hunters", "hunters after", "transformed folk")) {
            return Optional.of("Witch Hunters pursue vampires and werewolves, shoot silver bolts, then close with blades; distance is wiser than bravado.");
        }
        if (containsAny(normalized, "silver")
                && containsAny(normalized, "werewolf", "werewolves", "wolfman", "vampire", "vampires",
                "transformed folk", "transformed creatures")) {
            return Optional.of("Silver bites transformed flesh: Silver Swords and Silver Bolts hit vampires and werewolves harder.");
        }
        if (containsAny(normalized, "guard shouted", "guards shouted", "village guard", "village guards",
                "guard angry", "guards angry")) {
            return Optional.of("Village guards attack monsters, vampires, and werewolves; if one shouts, check what shape, curse, or company you are carrying.");
        }
        if (containsAny(normalized, "apothecary", "apothecaries")) {
            return Optional.of("Apothecaries are village potion trade and practical help, not coven witches; buy supplies, but do not expect circle secrets.");
        }
        return Optional.empty();
    }

    private static boolean asksRecipeRiteLearning(String normalized) {
        return containsAny(normalized, "learn brewing", "learn brews", "learn recipes", "learn a recipe",
                "brewing and rites", "brewing and rituals", "recipes and rites", "recipes and rituals",
                "learn brewing and rites", "learn witchcraft properly");
    }

    private static boolean asksRitePreparation(String normalized) {
        return containsAny(normalized, "learn before trying a rite", "learn before trying ritual",
                "what should i learn before trying a rite", "what should i learn before trying ritual",
                "before trying a rite", "before trying ritual", "before a rite", "before a ritual",
                "before doing a rite", "before doing ritual");
    }

    private static boolean asksExactRecipe(String normalized) {
        return containsAny(normalized, "exact recipe", "exact ingredients", "ingredient list", "ingredients for",
                "recipe for", "what is the recipe", "witch's oven recipe", "witches oven recipe");
    }

    private static boolean asksMissingRecipeCheck(String normalized) {
        return containsAny(normalized, "recipe is missing", "recipe missing", "exact recipe is missing",
                "exact recipe missing", "missing recipe", "recipe absent", "page is missing", "page missing");
    }

    private static boolean asksCauldronPrep(String normalized) {
        return containsAny(normalized, "before i touch the cauldron", "before touching the cauldron",
                "before the cauldron", "prepare the cauldron", "ready the cauldron", "need before")
                && containsAny(normalized, "cauldron", "kettle", "brew", "brewing");
    }

    private static boolean asksBasicBrewRecipe(String normalized) {
        return containsAny(normalized, "basic brew recipe", "basic recipe", "simple brew recipe",
                "explain a brew recipe", "explain basic brew", "how do recipes work")
                && containsAny(normalized, "brew", "brewing", "potion", "recipe");
    }

    private static boolean asksFumeUse(String normalized) {
        return containsAny(normalized, "fume", "fumes");
    }

    private static boolean asksFumeSafety(String normalized) {
        return containsAny(normalized, "fume", "fumes")
                && containsAny(normalized, "safely", "safe", "get", "gather", "catch", "collect");
    }

    private static boolean asksSpinningWheel(String normalized) {
        return containsAny(normalized, "spinning wheel", "spinning");
    }

    private static boolean asksDistillery(String normalized) {
        return containsAny(normalized, "distillery", "distill");
    }

    private static boolean asksOfferings(String normalized) {
        return containsAny(normalized, "offering", "offerings", "why offer", "what do i offer");
    }

    private static boolean asksWeakAltar(String normalized) {
        return containsAny(normalized, "altar is too weak", "altar too weak", "weak altar",
                "altar not strong", "altar lacks power", "not enough altar power");
    }

    private static boolean asksFailedRite(String normalized) {
        return containsAny(normalized, "rite fail", "rite failed", "ritual fail", "ritual failed",
                "why did my rite fail", "why did my ritual fail", "fizzled", "fizzle");
    }

    private static boolean asksFamiliarBindingRite(String normalized) {
        return containsAny(normalized, "familiar binding", "familiar rite", "familiar ritual",
                "bind a familiar", "binding rite")
                && containsAny(normalized, "familiar", "rite", "ritual", "circle");
    }

    private static boolean asksSummoningRite(String normalized) {
        return containsAny(normalized, "summoning rite", "summon rite", "summoning ritual", "summon ritual")
                || (containsAny(normalized, "summon", "summoning") && containsAny(normalized, "explain", "rite", "ritual"));
    }

    private static boolean asksCurseRite(String normalized) {
        return containsAny(normalized, "curse rite", "curse ritual", "cursing rite", "cursing ritual")
                || (containsAny(normalized, "curse", "cursed") && containsAny(normalized, "explain", "rite", "ritual"));
    }

    private static boolean asksRiteSafety(String normalized) {
        return containsAny(normalized, "never do during a rite", "never do in a rite", "never do during ritual",
                "should i never do", "what not to do")
                && containsAny(normalized, "rite", "ritual", "circle");
    }

    private static boolean asksBeginnerPath(String normalized) {
        return containsAny(normalized, "safe beginner path", "beginner path", "first recipe",
                "first rite", "first ritual", "start learning", "safe to learn first",
                "learn first", "what should i learn first", "what is safe to learn",
                "careful beginner", "beginner learn", "before touching your craft", "touching your craft");
    }

    private static boolean asksMixingWarning(String normalized) {
        return containsAny(normalized, "never mix", "should i never mix", "what should i never mix",
                "what not to mix", "bad mix", "dangerous mix");
    }

    private static boolean asksGrimoireSummary(String normalized) {
        return containsAny(normalized, "write in my grimoire", "write in the grimoire", "write down",
                "after this lesson", "grimoire after");
    }

    private static Optional<String> contextualFollowup(String normalized, String memoryContext, String playerName) {
        String memory = normalize(memoryContext);
        if (containsAny(normalized, "what did i just tell you", "what did i tell you",
                "what did i say about water", "about water")
                && containsAny(memory, "wary of deep water", "nervous around deep water",
                "afraid of deep water", "fear deep water")) {
            return Optional.of("You told me deep water makes you nervous, " + playerName + "; that is a sensible fear.");
        }
        if (containsAny(normalized, "my jars", "about my jars", "what do you remember about jars",
                "singing jars", "trust jars")
                && containsAny(memory, "wary of singing jars", "do not trust singing jars",
                "dont trust singing jars", "don't trust singing jars")) {
            return Optional.of("You do not trust singing jars, " + playerName + "; wise. Label them, then stop listening after the second note.");
        }
        if (containsAny(normalized, "what food did i say", "what did i say i prefer",
                "what did i say i preferred", "food did i prefer", "food do i prefer")
                && containsAny(memory, "prefers potatoes over eggs", "prefer potatoes over eggs",
                "likes potatoes")) {
            return Optional.of("You prefer potatoes over eggs, " + playerName + ". A sensible root beats a smug little moon.");
        }
        if (containsAny(normalized, "what do you remember about my plans", "my plans", "what was my plan")
                && containsAny(memory, "wants to start a coven", "start a coven")) {
            return Optional.of("You want to start a careful coven, " + playerName + "; start by earning trust, not collecting names.");
        }
        if (containsAny(normalized, "what did my village think", "what does my village think",
                "what did i say about my village")
                && containsAny(memory, "village thinks")) {
            return Optional.of("Your village thinks trouble walks near me, " + playerName + "; they are not entirely wrong.");
        }
        if (containsAny(normalized, "are they wrong", "were they wrong")
                && containsAny(memory, "village thinks you are trouble", "village think you are trouble")) {
            return Optional.of("Not entirely. Trouble follows me, " + playerName + "; I just keep mine better trained.");
        }
        if (containsAny(normalized, "did you bless me", "am i blessed", "your blessing", "you blessed me",
                "the luck", "that blessing", "blessing do anything")
                && containsAny(memory, "blessed with luck", "coven_witch_blessed")) {
            return Optional.of("A small luck, " + playerName + "; spend it before pride eats the shine off.");
        }
        if (containsAny(normalized, "did you curse me", "am i cursed", "your curse", "you cursed me", "still cursed")
                && containsAny(memory, "cursed with bad luck", "coven_witch_cursed")) {
            return Optional.of("A five-minute shadow, not a life sentence. Mind your tongue when it lifts.");
        }
        if (containsAny(normalized, "sorry", "apologize", "apology", "that was rude")
                && containsAny(memory, "cursed with bad luck", "insulting this witch")) {
            return Optional.of("Apology heard, " + playerName + "; let the shadow finish its walk, then mind your tongue.");
        }
        if (containsAny(normalized, "that herb", "the herb", "herb gift", "giving me a herb", "gave me a herb",
                "do you remember giving me a herb", "another herb", "more herbs")
                && containsAny(memory, "received one herb gift", "coven_witch_herb_gift")) {
            return Optional.of("I gave one herb freely, " + playerName + "; plant it, brew it, or owe it respect.");
        }
        if (containsAny(normalized, "what scares me", "what did i say scares me", "what did i say i fear",
                "what do i fear", "what am i afraid of", "what did i say makes me nervous",
                "makes me nervous", "nightmare", "nightmares", "bad dreams")
                && containsAny(memory, "wary of nightmares", "afraid of nightmares", "wary of nightmares",
                "fear nightmares", "fears nightmares")) {
            return Optional.of("I remember your nightmare fear, " + playerName + "; keep a Dream Catcher close and mark your way back.");
        }
        if (containsAny(normalized, "what did i say i fear", "what do i fear", "what am i afraid of")) {
            return Optional.of("You spoke of nightmares, " + playerName + "; keep a Dream Catcher close and mark your way back.");
        }
        if (containsAny(normalized, "what am i looking for", "what was i looking for", "still looking", "where start")
                && containsAny(memory, "looking for an arthana")) {
            return Optional.of("Still chasing the arthana, " + playerName + "? Ask after ritual blades and altar range, not table knives.");
        }
        if (containsAny(normalized, "that sounds creepy", "sounds creepy", "creepy")
                && containsAny(memory, "taglock", "poppet")) {
            return Optional.of("Creepy keeps fools careful. Taglocks are tools, not toys.");
        }
        if (containsAny(normalized, "how would i get back", "how do i get back", "get back", "return")
                && containsAny(memory, "spirit world", "sleeping brew", "nightmare")) {
            return Optional.of("Mark where you crossed, keep to return paths, and do not treat dreams like roads.");
        }
        if (containsAny(normalized, "come back after sunrise", "back after sunrise", "after sunrise")) {
            return Optional.of("Then come with daylight on your boots, " + playerName + "; night debts keep poor records.");
        }
        if (containsAny(normalized, "did you expect me back", "expected me back", "i came back", "came back")
                && containsAny(memory, "plans to come back after sunrise", "come back after sunrise")) {
            return Optional.of("I expected a shadow at sunrise; you brought boots. Good enough.");
        }
        if (containsAny(normalized, "village thinks you are trouble", "village think you are trouble")) {
            return Optional.of("Then your village has eyes. Trouble and I are old neighbors.");
        }
        if (containsAny(normalized, "any advice now", "advice now", "what now")) {
            if (containsAny(memory, "received one herb gift", "blessed with luck", "cursed with bad luck",
                    "looking for an arthana", "afraid of nightmares", "wary of nightmares")) {
                return Optional.of("Keep the gifted herb, keep a Dream Catcher close, and let the little shadow expire before boasting.");
            }
            return Optional.of("Keep salt dry, coin hidden, and questions sharper than fear.");
        }
        return Optional.empty();
    }

    private static boolean asksRememberRequest(String normalized) {
        return normalized.startsWith("remember ")
                && !normalized.contains("remember me")
                && !normalized.contains("what do you remember")
                && !normalized.contains("what you remember");
    }

    private static String rememberRequestAnswer(String normalized, String playerName) {
        String subject = normalized
                .replaceFirst("^remember\\s+(that\\s+|this\\s+)?", "")
                .trim();
        subject = playerMemoryPerspective(subject);
        if (subject.isBlank()) {
            return "I'll remember that, " + playerName + ".";
        }
        return NpcBrainResponse.sanitize("I'll remember that, " + playerName + ": " + subject + ".", 180);
    }

    private static String playerMemoryPerspective(String subject) {
        String text = subject == null ? "" : subject.trim();
        if (text.isBlank()) {
            return "";
        }
        text = text.replaceFirst("^i\\s+owe\\s+you\\b", "you owe me");
        text = text.replaceFirst("^i\\s+gave\\s+you\\b", "you gave me");
        text = text.replaceFirst("^i\\s+paid\\s+you\\b", "you paid me");
        text = text.replaceFirst("^i\\s+am\\s+afraid\\s+of\\b", "you are afraid of");
        text = text.replaceFirst("^i'm\\s+afraid\\s+of\\b", "you are afraid of");
        text = text.replaceFirst("^i\\s+am\\b", "you are");
        text = text.replaceFirst("^i'm\\b", "you are");
        text = text.replaceFirst("^i\\s+", "you ");
        text = text.replaceAll("\\bmy\\b", "your");
        return text.trim();
    }

    private static String tradeStockAnswer(String normalized) {
        String[] stock = {
                "witches' salt",
                "taglock kits",
                "fumes",
                "bat wool",
                "Revealing or Sleeping brews",
                "belladonna",
                "mandrake"
        };
        int start = Math.floorMod(normalized == null ? 0 : normalized.hashCode(), stock.length);
        StringBuilder answer = new StringBuilder("I trade ");
        for (int i = 0; i < stock.length; i++) {
            if (i > 0) {
                answer.append(i == stock.length - 1 ? ", and " : ", ");
            }
            answer.append(stock[(start + i) % stock.length]);
        }
        return answer.append("; right-click if you mean business.").toString();
    }

    private static String teachingAnswer(String normalized, String playerName) {
        if (containsAny(normalized, "learn witchcraft", "start learning")) {
            return "Open your Witches' Primer, " + playerName + "; its first lesson is ash, herbs, circles, and patience.";
        }
        if (containsAny(normalized, "rite", "ritual", "circle")) {
            return "First rite lesson, " + playerName + ": draw the right chalk, feed the altar, place only named offerings, then stand back.";
        }
        if (containsAny(normalized, "brew", "potion", "cauldron", "kettle")) {
            return "First brew lesson: know the effect you want before the kettle knows your name.";
        }
        if (containsAny(normalized, "coven")) {
            return "First coven lesson: a familiar opens doors, but a witch's trial decides if you may enter.";
        }
        return "First lesson: taglocks bind, jars matter, and every rite asks payment before it asks permission.";
    }

    private static String teachingKnowledgeAnswer(String knowledge, String normalized, String playerName) {
        String answer = knowledge == null ? "" : knowledge.trim();
        if (answer.isBlank()) {
            return teachingAnswer(normalized, playerName);
        }
        if (containsAny(normalized, "learn witchcraft", "start learning", "how do i start")
                && answer.contains("Primer")
                && !containsAny(normalize(answer), "next step", "next steps", "lesson", "first lesson")) {
            return NpcBrainResponse.sanitize(answer + " Next lesson: follow one opened Primer page before chasing secrets.",
                    NpcBrainResponse.MAX_SPEECH_LENGTH);
        }
        return answer;
    }

    private static boolean isSpecificAnswerPrompt(String normalized) {
        return asksPlayerName(normalized)
                || asksNpcName(normalized)
                || asksTradeDetails(normalized)
                || asksMemory(normalized)
                || asksCovenRecruiting(normalized)
                || asksTeachMe(normalized)
                || containsAny(normalized, "time", "where are we", "where am i", "witches salt",
                "taglock", "poppet", "belladonna", "mandrake", "arthana", "altar", "rite", "ritual",
                "brew", "potion", "spirit world");
    }

    static boolean asksMemory(String normalized) {
        return containsAny(normalized,
                "remember me", "remember about me", "remember of me", "what do you remember",
                "what you remember", "know about me", "do you know me", "what do you know about me",
                "have we met", "met before", "seen me before", "what did i say i fear",
                "what did i say scares me", "what did i say makes me nervous", "what did i say i was afraid");
    }

    static Optional<String> memoryAnswer(String memoryContext, String playerName) {
        String summary = memorySummary(memoryContext);
        if (summary.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(NpcBrainResponse.sanitize("I remember this, " + playerName + ": " + summary + ".", NpcBrainResponse.MAX_SPEECH_LENGTH));
    }

    private static String memorySummary(String memoryContext) {
        String cleaned = NpcBrainResponse.sanitize(memoryContext, 700);
        if (cleaned.isBlank()) {
            return "";
        }
        String longMemory = section(cleaned, "Long memory:", "Recent conversation:");
        String relationship = section(cleaned, "Relationship notes:", "Recent conversation:");
        String source = longMemory.isBlank() ? cleaned : longMemory;
        if (!relationship.isBlank()) {
            source = (source.isBlank() ? "" : source + " ") + relationship;
        }
        String rememberedPlayerName = playerNameFromMemory(cleaned);
        source = source
                .replaceAll("(?i)Entity memory:[^.]*\\.\\s*", "")
                .replaceAll("(?i)Recent conversation:", "")
                .replaceAll("(?i)Relationship notes:", "")
                .replaceAll("(?i)Long memory:", "")
                .replaceAll("player said \"[^\"]+\";\\s*", "")
                .replaceAll("witch answered \"[^\"]+\"\\.?", "")
                .replaceAll("(?i)\\b[A-Za-z0-9_]{2,24}(?:\\s+[A-Za-z0-9_]{2,24})?:\\s*[^;.!?]{0,140}(?:[;.!?]|$)", "");
        source = rewritePlayerMemoryPerspective(source, rememberedPlayerName)
                .replaceAll("(?i)without\\s+getting\\s+themselves\\s+killed", "without getting yourself killed")
                .replaceAll("(?i)looking\\s+for\\s+for\\b", "looking for")
                .replaceAll("(?i)([,;]\\s*)likes\\b", "$1you like")
                .replaceAll("(?i)([,;]\\s*)is\\s+wary\\s+of\\b", "$1you are wary of")
                .replaceAll("(?i)\\band\\s+is\\s+looking\\s+for\\b", "and you are looking for")
                .replaceAll("(?i)([,;]\\s*)is\\s+looking\\s+for\\b", "$1you are looking for")
                .replaceAll("(?i);\\s*(what\\s+now|now\\s+what|d)\\b", "")
                .replaceAll("(?i);\\s*none\\b", "")
                .replaceAll("[.!?]+\\s*", "; ")
                .replaceAll(";\\s*$", "")
                .replaceAll("\\s+", " ")
                .trim();
        return compactMemorySummary(source, 170);
    }

    private static String rewritePlayerMemoryPerspective(String source, String playerName) {
        String player = NpcBrainResponse.sanitize(playerName, 80);
        if (player.isBlank()) {
            return source;
        }
        String name = Pattern.quote(player);
        return source
                .replaceAll("(?i)\\b" + name + "\\s+received\\s+one\\s+herb\\s+gift\\s+from\\s+this\\s+witch\\b", "you received one herb gift from me")
                .replaceAll("(?i)\\b" + name + "\\s+was\\s+blessed\\s+with\\s+luck\\s+after\\s+praising\\s+this\\s+witch\\b", "you were blessed with luck after praising me")
                .replaceAll("(?i)\\b" + name + "\\s+was\\s+cursed\\s+with\\s+bad\\s+luck\\s+after\\s+insulting\\s+this\\s+witch\\b", "you were cursed with bad luck after insulting me")
                .replaceAll("(?i)\\b" + name + "\\s+has\\b", "you have")
                .replaceAll("(?i)\\b" + name + "\\s+likes\\b", "you like")
                .replaceAll("(?i)\\b" + name + "\\s+prefers\\b", "you prefer")
                .replaceAll("(?i)\\b" + name + "\\s+prefers\\s+to\\s+be\\s+called\\b", "you prefer to be called")
                .replaceAll("(?i)\\b" + name + "\\s+said\\s+they\\s+are\\b", "you said you are")
                .replaceAll("(?i)\\b" + name + "\\s+wants\\s+to\\s+learn\\b", "you want to learn")
                .replaceAll("(?i)\\b" + name + "\\s+wants\\s+to\\s+start\\b", "you want to start")
                .replaceAll("(?i)\\b" + name + "\\s+wants\\s+to\\s+become\\b", "you want to become")
                .replaceAll("(?i)\\b" + name + "\\s+is\\s+wary\\s+of\\b", "you are wary of")
                .replaceAll("(?i)\\b" + name + "\\s+is\\s+looking\\s+for\\b", "you are looking for")
                .replaceAll("(?i)\\b" + name + "\\s+is\\s+struggling\\s+with\\b", "you are struggling with")
                .replaceAll("(?i)\\b" + name + "\\s+plans\\s+to\\s+come\\s+back\\b", "you plan to come back")
                .replaceAll("(?i)\\b" + name + "'s\\s+village\\s+thinks\\b", "your village thinks");
    }

    private static String compactMemorySummary(String source, int maxLength) {
        String[] notes = source.split(";\\s*");
        StringBuilder summary = new StringBuilder();
        Set<String> seen = new LinkedHashSet<>();
        for (String raw : notes) {
            String note = raw.trim();
            if (note.isBlank()) {
                continue;
            }
            if (isBadMemorySummaryNote(note)) {
                continue;
            }
            String key = normalize(note);
            if (!seen.add(key)) {
                continue;
            }
            if (summary.isEmpty()) {
                if (note.length() <= maxLength) {
                    summary.append(note);
                }
                continue;
            }
            if (summary.length() + 2 + note.length() > maxLength) {
                continue;
            }
            summary.append("; ").append(note);
        }
        return NpcBrainResponse.sanitize(summary.toString(), maxLength);
    }

    private static boolean isBadMemorySummaryNote(String note) {
        String normalized = normalize(note);
        return normalized.length() <= 3
                || (!normalized.startsWith("you ") && !normalized.startsWith("your "))
                || normalized.startsWith("what ")
                || normalized.startsWith("how ")
                || normalized.startsWith("where ")
                || normalized.startsWith("when ")
                || normalized.startsWith("why ")
                || normalized.startsWith("can ")
                || normalized.startsWith("do ")
                || normalized.startsWith("does ")
                || normalized.startsWith("should ")
                || normalized.startsWith("would ")
                || normalized.contains("you asked")
                || normalized.contains("you ask")
                || normalized.contains("you have a question")
                || normalized.contains("you have a list")
                || normalized.contains("you have a short")
                || normalized.contains("you know ")
                || normalized.contains("you can repair")
                || normalized.contains("you are looking for a brew")
                || normalized.contains("you are looking for brew")
                || normalized.contains("you are looking for a potion")
                || normalized.contains("you are looking for help")
                || normalized.contains("you are curious")
                || normalized.contains("you have before entering")
                || normalized.contains("you have a herb before")
                || normalized.contains("you have herb before");
    }

    private static String playerNameFromMemory(String memoryContext) {
        Matcher matcher = Pattern.compile("(?i)player\\s+is\\s+([A-Za-z0-9_]{2,24})").matcher(memoryContext);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher fact = Pattern.compile("(?i)\\b([A-Za-z0-9_]{2,24})\\s+(?:has|likes|prefers\\s+to\\s+be\\s+called|said\\s+they\\s+are|wants\\s+to\\s+learn|is\\s+wary\\s+of|is\\s+looking\\s+for|is\\s+struggling\\s+with|plans\\s+to\\s+come\\s+back|received\\s+one\\s+herb\\s+gift|was\\s+blessed|was\\s+cursed)\\b")
                .matcher(memoryContext);
        return fact.find() ? fact.group(1) : "";
    }

    private static String section(String text, String startMarker, String endMarker) {
        String lower = text.toLowerCase(Locale.ROOT);
        int start = lower.indexOf(startMarker.toLowerCase(Locale.ROOT));
        if (start < 0) {
            return "";
        }
        int contentStart = start + startMarker.length();
        int end = lower.indexOf(endMarker.toLowerCase(Locale.ROOT), contentStart);
        return text.substring(contentStart, end < 0 ? text.length() : end).trim();
    }

    static String safeName(String playerName) {
        String cleaned = NpcBrainResponse.sanitize(playerName, 24).replaceAll("[^A-Za-z0-9_]", "");
        return cleaned.isBlank() ? "traveler" : cleaned;
    }

    static String safeDisplayName(String value, String fallback) {
        String cleaned = NpcBrainResponse.sanitize(value, 40)
                .replaceAll("[^A-Za-z0-9_ .'-]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private static Optional<String> sceneTime(String timeOfDay, String clock, long day) {
        String time = NpcBrainResponse.sanitize(timeOfDay, 24);
        String clockText = NpcBrainResponse.sanitize(clock, 12);
        if (time.isBlank() && clockText.isBlank() && day < 0L) {
            return Optional.empty();
        }
        StringBuilder builder = new StringBuilder("It is ");
        builder.append(time.isBlank() ? "some difficult hour" : time);
        if (!clockText.isBlank()) {
            builder.append(", about ").append(clockText);
        }
        if (day >= 0L) {
            builder.append(" on day ").append(day);
        }
        builder.append(".");
        return Optional.of(builder.toString());
    }

    private static Optional<String> scenePlace(String biome, String timeOfDay) {
        String place = NpcBrainResponse.sanitize(biome, 40);
        String time = NpcBrainResponse.sanitize(timeOfDay, 24);
        if (place.isBlank() && time.isBlank()) {
            return Optional.empty();
        }
        if (place.isBlank()) {
            return Optional.of("The hour says " + time + "; the land can answer for itself.");
        }
        if (time.isBlank()) {
            return Optional.of("We are in the " + place + ".");
        }
        return Optional.of("We are in the " + place + ", under " + time + " skies.");
    }

    static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsWord(String text, String word) {
        return Pattern.compile("(^|\\s)" + Pattern.quote(word) + "($|\\s)")
                .matcher(text)
                .find();
    }

    static boolean isGreeting(String normalized) {
        return normalized.matches(".*\\b(hello|hi|hey|greetings|good evening|good morning|good day)\\b.*");
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9' ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
