import java.util.*;
import java.io.*;
import java.math.*;
import java.awt.Point;

/**
 * Grab the pellets as fast as you can!
 **/
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int width = in.nextInt(); // size of the grid
        int height = in.nextInt(); // top left corner is (x=0, y=0)
        if (in.hasNextLine()) {
            in.nextLine();
        }
        Plateau plateau = new Plateau();
        Pastille defaut = new Pastille(-1,-1, 0);
        Map<Integer, Point> mesPacs = new HashMap<Integer, Point>();
        for (int i = 0; i < height; i++) {
            String row = in.nextLine(); // one line of the grid: space " " is floor, pound "#" is wall
            plateau.ajouterSol(row, i);
        }

        // game loop
        while (true) {
            mesPacs.clear();
            plateau.getPacs().clear();
            plateau.getPastilles().clear();
            int myScore = in.nextInt();
            int opponentScore = in.nextInt();
            int visiblePacCount = in.nextInt(); // all your pacs and enemy pacs in sight
            for (int i = 0; i < visiblePacCount; i++) {
                int pacId = in.nextInt(); // pac number (unique within a team)
                boolean mine = in.nextInt() != 0; // true if this pac is yours
                int x = in.nextInt(); // position in the grid
                int y = in.nextInt(); // position in the grid
                String typeId = in.next(); // unused in wood leagues
                int speedTurnsLeft = in.nextInt(); // unused in wood leagues
                int abilityCooldown = in.nextInt(); // unused in wood leagues
                Pac pac = new Pac(pacId, mine, x, y, typeId, speedTurnsLeft, abilityCooldown);
                plateau.getPacs().add(pac);
                if (mine) {
                    mesPacs.put(pacId, pac.position);
                }
                
            }
            int visiblePelletCount = in.nextInt(); // all pellets in sight
            for (int i = 0; i < visiblePelletCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int value = in.nextInt(); // amount of points this pellet is worth
                Pastille p = new Pastille(x, y, value);
                plateau.getPastilles().add(p);
                if (x == 0) {
                    plateau.getPastilles().add(new Pastille(width, y, value));
                }
                if (x == width - 1) {
                    plateau.getPastilles().add(new Pastille(-1, y, value));
                }
                if (y == 0) {
                    plateau.getPastilles().add(new Pastille(x, height, value));
                }
                if (y == height - 1) {
                    plateau.getPastilles().add(new Pastille(x, -1, value));
                }
            }

            
            Map<Integer, Pastille> destinations = new HashMap<Integer, Pastille>();
            List<String> actions = new ArrayList<String>();
            for (Pac p : plateau.getPacs()) {
                if (p.mine) {
                    plateau.sol.put(p.position, true);
                    if (visiblePelletCount == 0 && p.position.distance(new Point(width / 2, height / 2)) < 2) {
                        destinations.put(p.numero, new Pastille(0,0,1));
                    } else {
                        double distance = 100;
                        destinations.put(p.numero, null);
                        Point position = p.position;
                        // parcours pac adverses pour collisions
                        for (Pac adverse : plateau.getPacs()) {
                            if (!adverse.mine) {
                                if (adverse.position.distance(p.position) < 5) {
                                    if (p.isTypePacSuperieur(adverse)) {
                                        destinations.put(p.numero, new Pastille((int)adverse.position.getX(), (int)adverse.position.getY(), 1));
                                        break;
                                    } else {
                                        if (p.cooldown == 0) {
                                            String nouveauType = p.modifierType(adverse);
                                            if (!nouveauType.equals(p.typeId)) {
                                                actions.add("SWITCH " + p.numero + " " + nouveauType);
                                                destinations.put(p.numero, defaut);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (destinations.get(p.numero) == null) {
                            List<Pastille> proches = new ArrayList<Pastille>();
                        // parcours pastilles sinon
                            for (Pastille pastille : plateau.getPastilles()) {
                                double distanceTemp = position.distance(pastille.getPosition());
                                if (distanceTemp == distance && pastille.getPoints() == 10 && !contientDestination(destinations, pastille)) {
                                    destinations.put(p.numero, pastille);
                                } else if (distanceTemp < distance && !contientDestination(destinations, pastille)) {
                                    if (p.speedTurnsLeft > 0 && distanceTemp < 2) {
                                        proches.add(pastille);
                                        continue;
                                    }
                                    Point pointTemporaire = plateau.positionsPrecedentes.get(p.numero);
                                    if (!p.position.equals(pointTemporaire)) { 
                                        if (!autrePacPlusPres(mesPacs, p.numero, distanceTemp, pastille.getPosition())) {
                                            destinations.put(p.numero, pastille);
                                            distance = distanceTemp;
                                        }
                                    } else {
                                        if (!pastille.getPosition().equals(plateau.positionsPrecedentes.get(p.numero))) {
                                            if (!autrePacPlusPres(mesPacs, p.numero, distanceTemp, pastille.getPosition())) {
                                                destinations.put(p.numero, pastille);
                                                distance = distanceTemp;
                                            }
                                        }
                                    }
                                }
                            }
                            if (destinations.get(p.numero) == null || (destinations.get(p.numero).getPosition().distance(p.position) > 4 && p.speedTurnsLeft > 0)) {
                                if (!proches.isEmpty()) {
                                    destinations.put(p.numero, proches.get(0));
                                }
                            }
                        }
                        Point pointTemp = plateau.destinationsPrecedentes.get(p.numero);
                        if (destinations.get(p.numero) == null && pointTemp != null && pointTemp.equals(new Point(0,0))) {
                            System.err.println("Deplacement vers 0-0");
                            destinations.put(p.numero, new Pastille(0,0,1));
                        }
                        
                        if (destinations.get(p.numero) == null) {
                            Point point = plateau.trouverCasePlusProcheNonVisitee(p.position);
                            if (point != null) {
                                destinations.put(p.numero, new Pastille((int)point.getX(), (int)point.getY(), 1));
                            }
                        }

                        if (destinations.get(p.numero) != null && p.position.equals(plateau.positionsPrecedentes.get(p.numero))) {
                            if (p.cooldown == 0) {
                                actions.add("SWITCH " + p.numero + " " + p.modifierType(p));
                                destinations.put(p.numero, defaut);
                            }
                        }
                        
                        plateau.positionsPrecedentes.put(p.numero, p.position);
                        if (destinations.get(p.numero) != null && !defaut.equals(destinations.get(p.numero))){
                            if (p.cooldown == 0) {
                                actions.add("SPEED " + p.numero);
                                destinations.put(p.numero, defaut);
                            }
                        }
                    }
                }
            }

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");
            
            System.out.println(construireDeplacement(destinations, width, height, actions, defaut, plateau)) ; // MOVE <pacId> <x> <y>
        }
    }

    public static boolean autrePacPlusPres(Map<Integer, Point> mesPacs, int numero, double distance, Point position) {
        
        for (int key : mesPacs.keySet()) {
            if (key != numero) {
                if (mesPacs.get(key).distance(position) <= distance) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String construireDeplacement(Map<Integer, Pastille> destinations, int width, int height, List<String> actions, Pastille defaut, Plateau plateau) {
        List<String> dest = new ArrayList<String>();
        for (int i : destinations.keySet()) {
            if (destinations.get(i) != null) {
                if (!(destinations.get(i).equals(defaut))) {
                    System.err.println("Ajout MOVE pour " + i);
                    dest.add("MOVE " + i + " " + destinations.get(i).getPositionString(width, height));
                    plateau.destinationsPrecedentes.put(i, destinations.get(i).getPosition());
                }
            } else {
                System.err.println("Ajout MOVE par defaut pour " + i);
                dest.add("MOVE " + i + " " + width/2 + " " + height/2);
                plateau.destinationsPrecedentes.put(i, new Point(width/2, height/2));
            }
        }
        dest.addAll(actions);
        return String.join(" | ", dest);
    }

    public static boolean contientDestination(Map<Integer, Pastille> destinations, Pastille pastille) {
        for (Pastille p : destinations.values()) {
            if (p != null && p.equals(pastille)) {
                return true;
            }
        }
        return false;
    }
}

class Plateau
{
    List<Pastille> pastilles;
    List<Pac> pacs;
    Map<Integer, Point> positionsPrecedentes;
    Map<Integer, Point> destinationsPrecedentes;
    Map<Point, Boolean> sol;

    public Plateau() {
        pastilles = new ArrayList<Pastille>();
        pacs = new ArrayList<Pac>();
        positionsPrecedentes = new HashMap<Integer, Point>();
        destinationsPrecedentes = new HashMap<Integer, Point>();
        sol = new HashMap<Point, Boolean>();
    }

    public List<Pastille> getPastilles() {
        return pastilles;
    }

    public List<Pac> getPacs() {
        return pacs;
    }

    public void ajouterSol(String str, int numeroLigne) {
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ' ') {
                sol.put(new Point(i, numeroLigne), false);
            }
        }
    }

    public Point trouverCasePlusProcheNonVisitee(Point p) {
        double distance = 100;
        Point temp = null;
        for (Point point : sol.keySet()) {
            if (sol.get(point) == false) {
                if (point.distance(p) < distance) {
                    temp = point;
                    distance = point.distance(p);
                }
            }
        }
        return temp;
    }

}


class Pac
{
    int numero;
    boolean mine;
    Point position;
    String typeId;
    int speedTurnsLeft;
    int cooldown;

    public Pac() {

    }

    public Pac(int numero, boolean mine, int x, int y, String type, int turnsLeft, int cooldown) {
        this.numero = numero;
        this.mine = mine;
        this.position = new Point(x, y);
        this.typeId = type;
        this.speedTurnsLeft = turnsLeft;
        this.cooldown = cooldown;
    }

    public boolean isTypePacSuperieur(Pac p) {
        return ((this.typeId.equals("ROCK") && p.typeId.equals("SCISSORS"))
            || (this.typeId.equals("SCISSORS") && p.typeId.equals("PAPER"))
            || (this.typeId.equals("PAPER") && p.typeId.equals("ROCK")));
    }

    public String modifierType(Pac p) {
        String retour = "";
        if (p.typeId.equals("ROCK")) {
            retour = "PAPER";
        } else if (p.typeId.equals("SCISSORS")) {
            retour = "ROCK";
        } else {
            retour = "SCISSORS";
        }
        return retour;
    }
    

}

class Pastille
{
    Point position;
    int points;

    public Pastille() {
        this(0,0,0);
    }
    
    public Pastille(int x, int y, int points) {
        this.position = new Point(x, y);
        this.points = points;
    }
    
    public Point getPosition() {
        return position;
    }

    public int getPoints() {
        return points;
    }

    public String getPositionString(int width, int height) {
        return (((int)this.position.getX() + width) % width) + " " + (((int)this.position.getY() + height) % height);
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        return position.equals(((Pastille)o).position);
    }
}
