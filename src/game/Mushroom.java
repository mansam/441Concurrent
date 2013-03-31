package game;

import java.awt.Rectangle;
import java.awt.Point;
import gui.EntitySprite;


public class Mushroom implements Entity, Runnable{


	private Board 			m_board;
	private int 			m_curHealth;
	private Point			m_location;
	private EntitySprite 	m_sprite;
	private Rectangle 		m_boundingBox;
	
	//Maximum and starting health of the mushroom.
	private final int MAX_HEALTH 	= 4;
	
	//Length of one side of the Mushroom's bounding box (in pixels)
	private final int SQUARE_SIZE 	= 50;
	
	public Mushroom(Board board, Point location)
	{
		m_board 	= board;
		m_curHealth = MAX_HEALTH;
		m_location 	= location;
		
		recalcBoundingBox();
	}
	
	private void recalcBoundingBox()
	{
		int x = m_location.x - SQUARE_SIZE / 2;
		int y = m_location.y - SQUARE_SIZE / 2;
		m_boundingBox = new Rectangle( x, y, SQUARE_SIZE, SQUARE_SIZE );
	}
	
	
	public void move() 
	{
		//Mushrooms do not move
		return;
	}

	public void collidesWith(Entity entity) 
	{
		
	}

	public void die() 
	{
		m_curHealth--;
		if ( m_curHealth <= 0 )
		{
			//commented out until board is implemented
			//m_board.move( -1, -1, this );
		}
	}


	public EntitySprite getSprite() 
	{
		return m_sprite;
	}


	public Point getLocation()
	{
		return m_location;
	}


	public Rectangle getBoundingBox() 
	{
		return m_boundingBox;
	}


	public void run() 
	{
		while ( m_curHealth > 0 )
		{
			Thread.yield();
		}
	}

}