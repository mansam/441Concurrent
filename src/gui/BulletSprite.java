package gui;

import game.Bullet;
import game.Entity;

import java.awt.Image;
import java.awt.Rectangle;

public class BulletSprite extends EntitySprite {
	private Bullet myEntity;
	private static Image img;
	public BulletSprite(Bullet entityToManage) {
		super(entityToManage);
		this.myEntity=entityToManage;
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void cacheImages() {
		if (img != null) return;
		img = readImage("Bullet.bmp");

	}

	@Override
	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
	}

}
