import java.util.Random;
import java.util.Scanner;
import java.io.*;

/**
 * Main class for the Tile Matching Game.
 *
 * Game overview:
 *  - 5 tile sets (stacks, capacity 10) initialised with 1-10 random unique letters each.
 *  - A Reserve Queue  (26 letters, shuffled) – player can AddSet or ShiftQueue.
 *  - A Supplementary Queue (26 letters, shuffled) – auto-adds a tile every 3 steps.
 *  - High Score Table stored in HighScoreTable.txt (top 10).
 *
 * Commands:
 *  Match(i,j)  – compare top tiles of Set i and Set j
 *  AddSet(i)   – move front of Reserve Queue to top of Set i
 *  ShiftQueue  – rotate Reserve Queue (front element moves to rear)
 *  F           – end the game
 *
 * Only Stack and Queue data structures are used for game data storage.
 */
public class Main {

    // -----------------------------------------------------------------------
    // Game-state fields
    // -----------------------------------------------------------------------

    /** The 5 tile sets. */
    @SuppressWarnings("unchecked")
    static Stack<Character>[] sets = new Stack[5];

    /** Reserve queue: 26 shuffled letters. */
    static Queue<Character> reserveQueue;

    /** Supplementary queue: 26 shuffled letters. */
    static Queue<Character> supplementaryQueue;

    /** High-score table – two parallel queues (name / score). */
    static Queue<String>  hsNames;
    static Queue<Integer> hsScores;

    /** Running score of the current player. */
    static int score = 0;

    /** Remaining ShiftQueue rights (randomly assigned 1-5 at game start). */
    static int remainingShifts = 0;

    /** Number of valid steps taken so far. */
    static int stepCount = 0;

    /** Maximum allowed steps: (total initial tiles) * 1.2. */
    static int maxSteps = 0;

    /** Current player's name. */
    static String playerName = "";

    // -----------------------------------------------------------------------
    // main
    // -----------------------------------------------------------------------

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        Random rand = new Random();

        // ---- Player name -----------------------------------------------
        System.out.print("Enter player name: ");
        playerName = scanner.nextLine().trim();

        System.out.println("THE GAME STARTS NOW!...");
        System.out.println();

        // ---- Initialise the 5 sets -------------------------------------
        int totalInitialTiles = 0;
        for (int i = 0; i < 5; i++) {
            sets[i] = new Stack<>(10);
            // Create a full shuffled deck of 26 unique letters
            Queue<Character> deck = createShuffledLetterQueue(rand);
            int tileCount = rand.nextInt(10) + 1; // 1 – 10
            totalInitialTiles += tileCount;
            for (int j = 0; j < tileCount; j++) {
                sets[i].push(deck.dequeue());
            }
        }

        // Maximum steps = (total initial tiles) × 1.2  (integer truncation)
        maxSteps = (int)(totalInitialTiles * 1.2);

        // ---- Initialise queues -----------------------------------------
        reserveQueue      = createShuffledLetterQueue(rand);
        supplementaryQueue = createShuffledLetterQueue(rand);

        // Random 1-5 shift rights
        remainingShifts = rand.nextInt(5) + 1;

        // ---- Load high scores ------------------------------------------
        hsNames  = new Queue<>(10);
        hsScores = new Queue<>(10);
        loadHighScores();

        // ---- Display initial state -------------------------------------
        displayGameState();

        // ---- Game loop -------------------------------------------------
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                // EOF (e.g. redirected input ended)
                System.out.println("Game Over! Input ended.");
                displayGameState();
                break;
            }
            String cmd = scanner.nextLine().trim();

            // End-game command
            if (cmd.equals("F")) {
                System.out.println("Game Over! You chose to end the game.");
                displayGameState();
                break;
            }

            // Process the command; returns true only for syntactically valid commands
            boolean valid = processCommand(cmd);

            if (valid) {
                stepCount++;

                // Every 3 valid steps: automatic supplementary-queue addition
                if (stepCount % 3 == 0) {
                    handleAutoAddition();
                }

                // Display the updated state after each valid step
                displayGameState();

                // Check game-ending conditions
                if (stepCount >= maxSteps) {
                    System.out.println("Game Over! Maximum step limit (" + maxSteps + ") reached.");
                    break;
                }
                if (allSetsEmpty()) {
                    System.out.println("Game Over! All sets are empty.");
                    break;
                }
            }
        }

        // ---- End-game phase -------------------------------------------
        updateHighScores(playerName, score);
        saveHighScores();
        System.out.println();
        displayHighScores();

        scanner.close();
    }

    // -----------------------------------------------------------------------
    // Command processing
    // -----------------------------------------------------------------------

    /**
     * Parses and dispatches a single command.
     *
     * @param cmd trimmed input line from the player
     * @return true if the command is syntactically valid (should count as a step)
     */
    static boolean processCommand(String cmd) {

        // ---- Match(i,j) ------------------------------------------------
        if (cmd.startsWith("Match(") && cmd.endsWith(")")) {
            String inner = cmd.substring(6, cmd.length() - 1);
            String[] parts = inner.split(",");
            if (parts.length != 2) {
                System.out.println("Warning: Invalid Match command. Format: Match(i,j)");
                return false;
            }
            try {
                int i = Integer.parseInt(parts[0].trim());
                int j = Integer.parseInt(parts[1].trim());
                if (i < 1 || i > 5 || j < 1 || j > 5) {
                    System.out.println("Warning: Set indices must be between 1 and 5.");
                    return false;
                }
                handleMatch(i, j);
                return true;
            } catch (NumberFormatException e) {
                System.out.println("Warning: Invalid Match command – indices must be integers. Format: Match(i,j)");
                return false;
            }
        }

        // ---- AddSet(i) -------------------------------------------------
        if (cmd.startsWith("AddSet(") && cmd.endsWith(")")) {
            String inner = cmd.substring(7, cmd.length() - 1);
            try {
                int i = Integer.parseInt(inner.trim());
                if (i < 1 || i > 5) {
                    System.out.println("Warning: Set index must be between 1 and 5.");
                    return false;
                }
                handleAddSet(i);
                return true;
            } catch (NumberFormatException e) {
                System.out.println("Warning: Invalid AddSet command – index must be an integer. Format: AddSet(i)");
                return false;
            }
        }

        // ---- ShiftQueue ------------------------------------------------
        if (cmd.equals("ShiftQueue")) {
            handleShiftQueue();
            return true;
        }

        // ---- Unknown ---------------------------------------------------
        System.out.println("Warning: Unknown command. Valid commands: Match(i,j), AddSet(i), ShiftQueue, F");
        return false;
    }

    // -----------------------------------------------------------------------
    // Command handlers
    // -----------------------------------------------------------------------

    /**
     * Handles Match(i,j): compare top tiles of Set i and Set j.
     *   - Equal → remove both, +5 points.
     *   - Different → warning, no change.
     */
    static void handleMatch(int i, int j) {
        if (i == j) {
            System.out.println("Warning: Cannot match a set with itself. Choose two different sets.");
            return;
        }

        Stack<Character> setI = sets[i - 1];
        Stack<Character> setJ = sets[j - 1];

        if (setI.isEmpty()) {
            System.out.println("Warning: Set" + i + " is empty. Cannot match.");
            return;
        }
        if (setJ.isEmpty()) {
            System.out.println("Warning: Set" + j + " is empty. Cannot match.");
            return;
        }

        char topI = setI.peek();
        char topJ = setJ.peek();

        if (topI == topJ) {
            setI.pop();
            setJ.pop();
            score += 5;
            System.out.println("Match! '" + topI + "' removed from Set" + i + " and Set" + j
                    + ". +5 points! Score: " + score);
        } else {
            System.out.println("Warning: Tiles do not match. Set" + i + " has '" + topI
                    + "', Set" + j + " has '" + topJ + "'. Try again.");
        }
    }

    /**
     * Handles AddSet(i): dequeue front of Reserve Queue and push to Set i.
     *   - If no shift rights remain: -2 point penalty.
     *   - If Reserve Queue is empty: warning, no action.
     *   - If Set i is full: return the letter to the Reserve Queue, warning.
     */
    static void handleAddSet(int i) {
        // Penalty applies whenever AddSet is called with no shift rights left
        if (remainingShifts <= 0) {
            score -= 2;
            System.out.println("No shift rights remaining. AddSet penalty: -2 points! Score: " + score);
        }

        if (reserveQueue.isEmpty()) {
            System.out.println("Warning: Reserve Queue is empty. Cannot add tile to Set" + i + ".");
            return;
        }

        char letter = reserveQueue.dequeue();
        Stack<Character> target = sets[i - 1];

        if (target.isFull()) {
            reserveQueue.enqueue(letter);
            System.out.println("Warning: Set" + i + " is full (max 10 tiles). '"
                    + letter + "' returned to Reserve Queue.");
        } else {
            target.push(letter);
            System.out.println("'" + letter + "' added to top of Set" + i + ".");
        }
    }

    /**
     * Handles ShiftQueue: move front element of Reserve Queue to rear.
     *   - If no shift rights left: warning, no action, step still counted.
     *   - If Reserve Queue is empty: warning.
     */
    static void handleShiftQueue() {
        if (remainingShifts <= 0) {
            System.out.println("Warning: No shift rights remaining. Cannot shift Reserve Queue.");
            return;
        }
        if (reserveQueue.isEmpty()) {
            System.out.println("Warning: Reserve Queue is empty. Nothing to shift.");
            return;
        }
        char front = reserveQueue.dequeue();
        reserveQueue.enqueue(front);
        remainingShifts--;
        System.out.println("Reserve Queue shifted. '" + front + "' moved to rear."
                + " Remaining shifts: " + remainingShifts);
    }

    /**
     * Automatic action triggered every 3 valid steps:
     * Dequeue front of Supplementary Queue and push onto the set with the
     * fewest tiles (ties broken by lowest index).
     * If that set is full, return the letter to the Supplementary Queue.
     */
    static void handleAutoAddition() {
        if (supplementaryQueue.isEmpty()) {
            System.out.println("[Auto] Supplementary Queue is empty – no automatic addition this step.");
            return;
        }

        // Find the set with the minimum number of tiles
        int minSize  = sets[0].size();
        int minIndex = 0;
        for (int i = 1; i < 5; i++) {
            if (sets[i].size() < minSize) {
                minSize  = sets[i].size();
                minIndex = i;
            }
        }

        char letter = supplementaryQueue.dequeue();

        if (sets[minIndex].isFull()) {
            supplementaryQueue.enqueue(letter);
            System.out.println("[Auto] Set" + (minIndex + 1) + " is full. '"
                    + letter + "' returned to Supplementary Queue.");
        } else {
            sets[minIndex].push(letter);
            System.out.println("[Auto] '" + letter + "' added to Set" + (minIndex + 1)
                    + " (fewest tiles: " + minSize + ").");
        }
    }

    // -----------------------------------------------------------------------
    // Display helpers
    // -----------------------------------------------------------------------

    /**
     * Prints the full game state: all sets, both queues, score line.
     */
    static void displayGameState() {
        System.out.println();

        // All 5 sets
        for (int i = 0; i < 5; i++) {
            displaySet(i + 1, sets[i]);
        }
        System.out.println();

        // Reserve queue
        System.out.println("Reserve Queue:");
        displayQueue(reserveQueue);
        System.out.println();

        // Supplementary queue
        System.out.println("Supplementary Queue:");
        displayQueue(supplementaryQueue);
        System.out.println();

        // Status line
        System.out.println("Score: " + score
                + " | Remaining Shifts: " + remainingShifts
                + " | Step: " + stepCount + "/" + maxSteps);
        System.out.println();
    }

    /**
     * Prints a single set from top to bottom.
     *
     * Example (3 tiles A=bottom, B, C=top):
     *   Set1: Top → C
     *               B
     *               A
     *         ← Bottom
     *
     * The stack is fully restored after display.
     */
    static void displaySet(int setNum, Stack<Character> stack) {
        System.out.print("Set" + setNum + ": ");

        if (stack.isEmpty()) {
            System.out.println("(empty)");
            return;
        }

        /*
         * Algorithm (does not rely on any extra data structure beyond a second Stack):
         *  1. Pop each element from the original stack one by one, print it, and
         *     push it onto a temporary stack.  Because pop() yields elements in
         *     top-to-bottom order, the printed sequence is already correct.
         *  2. Pop everything from the temp stack and push it back onto the original
         *     stack.  Because the temp stack now holds elements in bottom-to-top
         *     order of the original, popping and re-pushing restores the original.
         */
        Stack<Character> temp = new Stack<>(10);
        boolean firstLine = true;

        // Step 1 – pop & print (top → bottom order) and save to temp
        while (!stack.isEmpty()) {
            char c = stack.pop();
            if (firstLine) {
                // "Set1: " = 6 chars, "Top → " = 6 chars  → tile at column 13
                System.out.println("Top → " + c);
                firstLine = false;
            } else {
                // 12 spaces to align tile under the first one
                System.out.println("            " + c);
            }
            temp.push(c);
        }

        // Step 2 – restore original stack from temp
        while (!temp.isEmpty()) {
            stack.push(temp.pop());
        }

        // 6 spaces to align "←" under "T" in "Top"
        System.out.println("      ← Bottom");
    }

    /**
     * Prints a queue from front to rear on a single line.
     * The queue is fully restored after display.
     *
     * Example: "Front → A B C D ← Rear"
     */
    static void displayQueue(Queue<Character> queue) {
        System.out.print("Front → ");

        if (queue.isEmpty()) {
            System.out.println("(empty) ← Rear");
            return;
        }

        int sz = queue.size();
        // Temp queue to allow full traversal without losing data
        Queue<Character> temp = new Queue<>(sz);

        for (int i = 0; i < sz; i++) {
            char c = queue.dequeue();
            if (i > 0) System.out.print(" ");
            System.out.print(c);
            temp.enqueue(c);
        }
        System.out.println(" ← Rear");

        // Restore original queue
        while (!temp.isEmpty()) {
            queue.enqueue(temp.dequeue());
        }
    }

    /**
     * Prints the high score table (name on one line, score on the next).
     * The queues are fully restored after display.
     */
    static void displayHighScores() {
        System.out.println("High Score Table");

        int count = hsNames.size();
        if (count == 0) {
            System.out.println("(No scores recorded yet)");
            return;
        }

        Queue<String>  tempNames  = new Queue<>(count);
        Queue<Integer> tempScores = new Queue<>(count);

        for (int i = 0; i < count; i++) {
            String name = hsNames.dequeue();
            int    sc   = hsScores.dequeue();
            System.out.println(name);
            System.out.println(sc);
            tempNames.enqueue(name);
            tempScores.enqueue(sc);
        }

        // Restore
        while (!tempNames.isEmpty()) {
            hsNames.enqueue(tempNames.dequeue());
            hsScores.enqueue(tempScores.dequeue());
        }
    }

    // -----------------------------------------------------------------------
    // High score file I/O
    // -----------------------------------------------------------------------

    /**
     * Reads high scores from HighScoreTable.txt.
     * File format: player name on one line, integer score on the next line.
     */
    static void loadHighScores() {
        try {
            File file = new File("HighScoreTable.txt");
            if (!file.exists()) return;

            BufferedReader br = new BufferedReader(new FileReader(file));
            String nameLine;
            int count = 0;
            while ((nameLine = br.readLine()) != null && count < 10) {
                String scoreLine = br.readLine();
                if (scoreLine == null) break;
                try {
                    int sc = Integer.parseInt(scoreLine.trim());
                    hsNames.enqueue(nameLine.trim());
                    hsScores.enqueue(sc);
                    count++;
                } catch (NumberFormatException e) {
                    // Skip malformed entry
                }
            }
            br.close();
        } catch (IOException e) {
            // Cannot read file – start with an empty table
        }
    }

    /**
     * Inserts the current player into the high score table and trims to top 10.
     *
     * Sorting rule: descending by score; ties are broken by insertion time
     * (newer player appears above the older one with the same score).
     */
    static void updateHighScores(String name, int newScore) {
        // Up to 10 existing + 1 new = 11 at most temporarily
        Queue<String>  tempNames  = new Queue<>(11);
        Queue<Integer> tempScores = new Queue<>(11);

        // Move all entries with strictly higher scores to tempNames/tempScores
        while (!hsScores.isEmpty() && hsScores.peek() > newScore) {
            tempNames.enqueue(hsNames.dequeue());
            tempScores.enqueue(hsScores.dequeue());
        }

        // Insert new player HERE (before entries with the same score → newer is first)
        tempNames.enqueue(name);
        tempScores.enqueue(newScore);

        // Append remaining entries (score <= newScore)
        while (!hsScores.isEmpty()) {
            tempNames.enqueue(hsNames.dequeue());
            tempScores.enqueue(hsScores.dequeue());
        }

        // Rebuild main queues keeping only the top 10
        hsNames  = new Queue<>(10);
        hsScores = new Queue<>(10);
        int count = 0;
        while (!tempNames.isEmpty() && count < 10) {
            hsNames.enqueue(tempNames.dequeue());
            hsScores.enqueue(tempScores.dequeue());
            count++;
        }
    }

    /**
     * Saves the current high score table to HighScoreTable.txt.
     * Format: player name on one line, integer score on the next.
     */
    static void saveHighScores() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("HighScoreTable.txt"));

            int count = hsNames.size();
            // Temp queues for restore (capacity ≥ 1 even if table is empty)
            Queue<String>  tempNames  = new Queue<>(count == 0 ? 1 : count);
            Queue<Integer> tempScores = new Queue<>(count == 0 ? 1 : count);

            for (int i = 0; i < count; i++) {
                String name = hsNames.dequeue();
                int    sc   = hsScores.dequeue();
                pw.println(name);
                pw.println(sc);
                tempNames.enqueue(name);
                tempScores.enqueue(sc);
            }
            pw.close();

            // Restore in-memory queues
            while (!tempNames.isEmpty()) {
                hsNames.enqueue(tempNames.dequeue());
                hsScores.enqueue(tempScores.dequeue());
            }
        } catch (IOException e) {
            System.out.println("Error: Could not save high scores. " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    /**
     * Creates and returns a Queue containing all 26 English letters in a
     * uniformly random order (Fisher-Yates shuffle).
     */
    static Queue<Character> createShuffledLetterQueue(Random rand) {
        char[] letters = new char[26];
        for (int i = 0; i < 26; i++) {
            letters[i] = (char)('A' + i);
        }
        // Fisher-Yates in-place shuffle
        for (int i = 25; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            char tmp = letters[i];
            letters[i] = letters[j];
            letters[j] = tmp;
        }
        Queue<Character> q = new Queue<>(26);
        for (char c : letters) {
            q.enqueue(c);
        }
        return q;
    }

    /**
     * Returns true if every set is empty.
     */
    static boolean allSetsEmpty() {
        for (int i = 0; i < 5; i++) {
            if (!sets[i].isEmpty()) return false;
        }
        return true;
    }
}
