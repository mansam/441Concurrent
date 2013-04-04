package game;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents the playing field in a game of Centipede. The Board is responsible
 * for resolving the movement of Entities, including collisions.
 */
public class Board {

	/**
	 * The standard width and height of a grid cell on the board. All entities
	 * should use this, not their own definition.
	 * 
	 * Values are based on size of the actual arcade game's play area. (15x16
	 * grid of tiles)
	 */
	public static final int TILE_SIZE = 16;
	public static final int WIDTH_PIXELS = 240;
	public static final int HEIGHT_PIXELS = 256;

	/**
	 * Though movement is done at a pixel resolution, the play area is broken up
	 * into small tile areas that are suitable for performing collision
	 * detection. This is very similar to how old arcade games actually dealt
	 * with collision. (PacMan's movement and collision detection are excellent
	 * examples of this.)
	 */
	private ReentrantLock[][] tiles;
	/**
	 * The board keeps track of all of the entities that are inside the field of
	 * play for the purpose of tracking collisions.
	 */
	private CopyOnWriteArrayList<Entity> entities;

	public Board() {
		tiles = new ReentrantLock[WIDTH_PIXELS / TILE_SIZE][HEIGHT_PIXELS
				/ TILE_SIZE];
		entities = new CopyOnWriteArrayList<Entity>();
		for (int x = 0; x < tiles.length; x++) {
			for (int y = 0; y < tiles[x].length; y++)
				tiles[x][y] = new ReentrantLock();
		}
	}

	/**
	 * Return the tile lock guarding the area encompassing the passed in (x,y)
	 * coords.
	 * 
	 * @param x
	 *            x coordinate
	 * @param y
	 *            y coordinate
	 * @return the reentrant lock guarding the tile
	 */
	private ReentrantLock getTile(int x, int y) {
		/* if x or y are < TILE_SIZE,
		 this truncates the division and indexes 0.*/
		return tiles[x / TILE_SIZE][y / TILE_SIZE];
	}

	/**
	 * Resolve movement of an entity to a particular x,y coordinate location on
	 * the play area. Movement happens on a pixel by pixel basis but collisions
	 * are resolved by visual tile area.
	 * 
	 * @param x
	 *            entity's intended goal x coordinate
	 * @param y
	 *            entity's intended goal y coordinate
	 * @param entity
	 *            the entity attempting to move
	 */
	public void move(int x, int y, Entity entity) {

		/* Should be safe to ask the entity for its location, considering
		 that it's not going to move while it's moving.*/
		int[] currLoc = entity.getLocation();
		/* if it's trying to go out of bounds, make its
		 destination == its current location.
		 This should be fine with the below code,
		 since the locks are reentrant.*/
		boolean wallCollision = false;
		ReentrantLock currentTile;
		ReentrantLock goalTile;
		boolean startedOutOfBounds = false;

		// catch things that have been created out of bounds.
		if (isOOB(currLoc[0], currLoc[1])) {
			currentTile = getTile(0, 0);
			startedOutOfBounds = true;
		} else {
			currentTile = getTile(currLoc[0], currLoc[1]);
		}

		if (isOOB(x, y)) {// going out of bounds
			wallCollision = true;
			if (startedOutOfBounds)	/*started out and going out, there's no hope
									 * for you. Cancel. */
				return;
			goalTile = getTile(currLoc[0], currLoc[1]);	/* started out but going
														 * in. */
		} else {
			goalTile = getTile(x, y);

		}

		// lock current tile and goal tile-- this will
		// prevent whoever is in the goal tile from moving away
		currentTile.lock();
		goalTile.lock();

		// okay, move where you wanted to move,
		// and unlock the space you no longer occupy.
		entity.updateLocation(x, y);
		currentTile.unlock();

		ArrayList<Entity> collisions = new ArrayList<Entity>();
		// find and resolve collections
		synchronized (entities) {
			for (Entity other : entities) {
				// yes, compare by reference, can't collide with oneself.
				if (other != entity) {
					if (entity.getBoundingBox()
							.overlaps(other.getBoundingBox())) {
						/* resolve collisions for current occupants of the tile,
						 but wait until the end to handle them for the moving
						 entity to make the rare occasional chain reaction a
						 little simpler.
						 this won't be *perfect* for things like tiny bullets,
						 but it'll be
						 acceptably close.*/
						if (entity.getType() != other.getType()) /* just
																  * suppress
																  * some
																  * useless
																  * messages
																  */
							System.out.println(entity + " collided with "
									+ other);
						other.collidesWith(entity.getType());
						collisions.add(other);
					}
				}
			}
		}

		/* 
		 * We can unlock the goal tile now because the entity has now moved into
		 * that space and we know what it collided with. It's important to
		 * unlock this tile first to avoid deadlock scenarios caused by possibly
		 * complicated collision resolution involving more than two entities.
		 */
		goalTile.unlock();

		// now resolve all the collisions for the moved entity
		for (Entity collision : collisions) {
			entity.collidesWith(collision.getType());
		}

		if (wallCollision) {
			System.out.println(entity + " collided with the wall");
			entity.collidesWith(EntityTypes.MUSHROOM);
		}
	}

	/**
	 * Are these coordinates outside the Board?
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static boolean isOOB(int x, int y) {
		return (x < 0 || x >= (WIDTH_PIXELS))
				|| (y < 0 || y >= (HEIGHT_PIXELS));
	}

	/**
	 * Place a new entity, such as a brand new mushroom, centipede, etc, on the
	 * play board. Then run the entity.
	 * 
	 * @param x
	 *            the x coord at which to create the entity
	 * @param y
	 *            the y coord at which to create the entity
	 * @param type
	 *            the type of entity to add to the board
	 */
	public Entity createEntity(int x, int y, EntityTypes type) {
		Entity newEntity = null;
		java.awt.Point p = new java.awt.Point(x, y);
		switch (type) {
		case MUSHROOM:
			newEntity = Mushroom.createMushroom(this, p);
			break;
		case PLAYER:
			newEntity = Player.createPlayer(this, p);
			break;
		case BULLET:
			newEntity = Bullet.createBullet(this, p);
			break;
		case CENTIPEDE:
			newEntity = Centipede.makeCentipede(Centipede.DEFAULT_CHAIN_LENGTH,
					this, new Point(x, y));
			break;
		}
		synchronized (entities) {
			entities.add(newEntity);
		}
		// Mushrooms don't need a thread, don't run them.
		if (!(type == EntityTypes.MUSHROOM)) {
			(new Thread(newEntity)).start();/* start the entity in it's own
											 * thread. */
		}

		return newEntity;

	}

	/**
	 * Add a centipede segment to the field, but don't start it as its own
	 * thread.
	 * 
	 * @param c the centipede segment to add.
	 */
	public void addCentipedeBody(Centipede c) {
		synchronized (entities) {
			entities.add(c);
		}
	}

	/**
	 * Safely remove an entity from the board.
	 * 
	 * @param e the entity to remove
	 */
	public void removeEntity(Entity e) {
		synchronized (entities) {
			entities.remove(e);
		}
	}

	/**
	 * Return a list of all the entities currently on the board.
	 * 
	 * @return an immutable list of all entities
	 */
	public Iterable<Entity> getAllEntities() {
		return Collections.unmodifiableList(entities); // best if this can't be
														// changed.
	}
}
