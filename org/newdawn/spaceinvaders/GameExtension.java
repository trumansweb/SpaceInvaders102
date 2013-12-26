package org.newdawn.spaceinvaders;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class GameExtension extends Game implements MouseListener {

	private static final long serialVersionUID = 3277516400247900760L;
	
	/** True if the right mouse key is currently pressed */
	private boolean Fire2Pressed = false;
	/** True if the right mouse key is currently pressed */
	private boolean PausePressed = false;
	/** True if the up cursor key is currently pressed */
	private boolean upPressed = false;
	/** True if the down cursor key is currently pressed */
	private boolean downPressed = false;
	/** Current Level */
	private int level = 1;
	
	public GameExtension() {
		System.out.println("Extension loaded");
		// mouse interaction
		addMouseListener(this);		
		// remove all key listeners the super constructor loads
		for(KeyListener l : super.getKeyListeners()) super.removeKeyListener(l);
		// listen only this class
		addKeyListener(new KeyInputHandler());
		setFiringInterval(100);
		
		setWindowTitle("Space Invaders 102.1");
		getContainer().remove(getContainer());
		// create a frame to contain our game
		setContainer(new JFrame("Space Invaders 102.1"));
		
		// get hold the content of the frame and set up the resolution of the game
		JPanel panel = (JPanel) getContainer().getContentPane();
		panel.setPreferredSize(new Dimension(800,600));
		panel.setLayout(null);
		
		// setup our canvas size and put it into the content of the frame
		setBounds(0,0,800,600);
		panel.add(this);
		
		// Tell AWT not to bother repainting our canvas since we're
		// going to do that our self in accelerated mode
		setIgnoreRepaint(true);
		
		// finally make the window visible 
		getContainer().pack();
		getContainer().setResizable(false);
		getContainer().setVisible(true);
		
		// add a listener to respond to the user closing the window. If they
		// do we'd like to exit the game
		getContainer().addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		
		// request the focus so key events come to us
		requestFocus();

		// create the buffering strategy which will allow AWT
		// to manage our accelerated graphics
		createBufferStrategy(2);
		setStrategy(getBufferStrategy());
		// clear entities the super constructor loads
		getEntities().clear();
		// and load level 1
		initEntities();
	}
	
	/**
	 * Initialize the starting state of the entities (ship and aliens). Each
	 * entity will be added to the overall list of entities in the game.
	 */
	private void initEntities() {
		// create the player ship and place it roughly in the center of the screen
		setShip(new ShipEntity(this,"sprites/ship.gif",370,550));
		getEntities().add(getShip());
		
		// create a block of aliens (5 rows, by 12 aliens, spaced evenly)
		setAlienCount(0);
			for (int x=0;x<getLevel();x++) {
				Entity alien = new AlienEntity(this,100+(x*50),(50)+30);
				getEntities().add(alien);
				setAlienCount(getAlienCount() + 1);
			}
	}
	
	/**
	 * Start a fresh game, this should clear out any old data and
	 * create a new set.
	 */
	private void startGame() {
		// clear out any existing entities and initialize a new set
		getEntities().clear();
		this.initEntities();
		// blank out any keyboard settings we might currently have
		setLeftPressed(false);
		setRightPressed(false);
		setUpPressed(false);
		setDownPressed(false);
		setFirePressed(false);
	}
	/**
	 * Notification that an alien has been killed
	 */
	public void notifyAlienKilled() {
		// reduce the alien count, if there are none left, the player levels up!
		setAlienCount(getAlienCount() - 1);
		
		if (getAlienCount() == 0) {
			levelUp();
		}
	}
		
	private void levelUp() {
		setLevel(getLevel() + 1);
		startGame();
	}
	
	/**
	 * Attempt to fire a shot from the player. Its called "try"
	 * since we must first check that the player can fire at this 
	 * point, i.e. has he/she waited long enough between shots
	 */
	public void tryToFire() {
		// check that we have waiting long enough to fire
		if (System.currentTimeMillis() - getLastFire() < getFiringInterval()) {
			return;
		}
		
		// if we waited long enough, create the shot entity, and record the time.
		setLastFire(System.currentTimeMillis());
		ShotEntity shot = new ShotEntity(this,"sprites/shot.gif",getShip().getX()+10,getShip().getY()-30);
		getEntities().add(shot);
		setFirePressed(false);
	}
	
	/**
	 * Attempt to fire a shot from the player. Its called "try"
	 * since we must first check that the player can fire at this 
	 * point, i.e. has he/she waited long enough between shots
	 */
	public void tryToFire2() {
		// check that we have waiting long enough to fire
		if (System.currentTimeMillis() - getLastFire() < getFiringInterval()) {
			return;
		}
		
		// if we waited long enough, create the shot entity, and record the time.
		setLastFire(System.currentTimeMillis());
		ShotEntity shot = new ShotEntity(this,"sprites/shot.gif",getShip().getX()+2,getShip().getY()-22);
		getEntities().add(shot);
		ShotEntity shot2 = new ShotEntity(this,"sprites/shot.gif",getShip().getX()+18,getShip().getY()-22);
		getEntities().add(shot2);
		setFire2Pressed(false);
	}
	
	/**
	 * A class to handle keyboard input from the user. The class
	 * handles both dynamic input during game play, i.e. left/right 
	 * and shoot, and more static type input (i.e. press any key to
	 * continue)
	 * 
	 * This has been implemented as an inner class more through 
	 * habbit then anything else. Its perfectly normal to implement
	 * this as seperate class if slight less convienient.
	 * 
	 * @author Kevin Glass
	 */
	private class KeyInputHandler extends KeyAdapter {
		/** The number of key presses we've had while waiting for an "any key" press */
		private int pressCount = 1;
		private boolean fireKeyReleased = true;
		/**
		 * Notification from AWT that a key has been pressed. Note that
		 * a key being pressed is equal to being pushed down but *NOT*
		 * released. Thats where keyTyped() comes in.
		 *
		 * @param e The details of the key that was pressed 
		 */
		public void keyPressed(KeyEvent e) {
			// if we're waiting for an "any key" typed then we don't 
			// want to do anything with just a "press"
			if (isWaitingForKeyPress()) return;
			
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				setLeftPressed(true);
			}
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				setRightPressed(true);
			}
			if (e.getKeyCode() == KeyEvent.VK_UP) {
				setUpPressed(true);
			}
			if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				setDownPressed(true);
			}
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				if(fireKeyReleased) setFirePressed(true);
				fireKeyReleased = false;
			}
		} 
		
		/**
		 * Notification from AWT that a key has been released.
		 *
		 * @param e The details of the key that was released 
		 */
		public void keyReleased(KeyEvent e) {
			// if we're waiting for an "any key" typed then we don't 
			// want to do anything with just a "released"
			if (isWaitingForKeyPress()) {
				return;
			}
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				setLeftPressed(false);
			}
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				setRightPressed(false);
			}
			if (e.getKeyCode() == KeyEvent.VK_UP) {
				setUpPressed(false);
			}
			if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				setDownPressed(false);
			}
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				setFirePressed(false);
				fireKeyReleased = true;
			}
		}

		/**
		 * Notification from AWT that a key has been typed. Note that
		 * typing a key means to both press and then release it.
		 *
		 * @param e The details of the key that was typed. 
		 */
		public void keyTyped(KeyEvent e) {
			// if we're waiting for a "any key" type then
			// check if we've received any recently. We may
			// have had a keyType() event from the user releasing
			// the shoot or move keys, hence the use of the "pressCount"
			// counter.
			if (String.valueOf(e.getKeyChar()).equalsIgnoreCase("p")) {
				if(isPausePressed()) setPausePressed(false);
				else setPausePressed(true);
			}
			if (isWaitingForKeyPress()) {
				if (pressCount == 1) {
					// since we've now received our key typed
					// event we can mark it as such and start 
					// our new game
					setWaitingForKeyPress(false);
					startGame();
					pressCount = 0;
				} else {
					pressCount++;
				}
			}
			
			// if we hit escape, then quit the game
			if (e.getKeyChar() == 27) {
				System.exit(0);
			}
		}
	}
	
	/**
	 * The main game loop. This loop is running during all game
	 * play as is responsible for the following activities:
	 * <p>
	 * - Working out the speed of the game loop to update moves
	 * - Moving the game entities
	 * - Drawing the screen contents (entities, text)
	 * - Updating game events
	 * - Checking Input
	 * <p>
	 */
	public void gameLoop() {
		long lastLoopTime = SystemTimer.getTime();
		
		// keep looping round til the game ends
		while (isGameRunning()) {
			// work out how long its been since the last update, this
			// will be used to calculate how far the entities should
			// move this loop
			long delta = SystemTimer.getTime() - lastLoopTime;
			lastLoopTime = SystemTimer.getTime();

			// update the frame counter
			setLastFpsTime(getLastFpsTime() + delta);
			setFps(getFps() + 1);
			
			// update our FPS counter if a second has passed since
			// we last recorded
			if (getLastFpsTime() >= 1000) {
				getContainer().setTitle(getWindowTitle()+" (FPS: "+getFps()+")");
				setLastFpsTime(0);
				setFps(0);
			}
			// Get hold of a graphics context for the accelerated 
			// surface and blank it out
			Graphics2D g = (Graphics2D) getStrategy().getDrawGraphics();
			g.setColor(Color.black);
			g.fillRect(0,0,800,600);
			
			// cycle round asking each entity to move itself
			if (!isWaitingForKeyPress() && !isPausePressed()) {
				for (int i=0;i<getEntities().size();i++) {
					Entity entity = (Entity) getEntities().get(i);
					
					entity.move(delta);
				}
			}
			
			// cycle round drawing all the entities we have in the game
			for (int i=0;i<getEntities().size();i++) {
				Entity entity = (Entity) getEntities().get(i);
				
				entity.draw(g);
			}
			
			// brute force collisions, compare every entity against
			// every other entity. If any of them collide notify 
			// both entities that the collision has occured
			for (int p=0;p<getEntities().size();p++) {
				for (int s=p+1;s<getEntities().size();s++) {
					Entity me = (Entity) getEntities().get(p);
					Entity him = (Entity) getEntities().get(s);
					
					if (me.collidesWith(him)) {
						me.collidedWith(him);
						him.collidedWith(me);
					}
				}
			}
			
			// remove any entity that has been marked for clear up
			getEntities().removeAll(getRemoveList());
			getRemoveList().clear();

			// if a game event has indicated that game logic should
			// be resolved, cycle round every entity requesting that
			// their personal logic should be considered.
			if (isLogicRequiredThisLoop()) {
				for (int i=0;i<getEntities().size();i++) {
					Entity entity = (Entity) getEntities().get(i);
					entity.doLogic();
				}
				
				setLogicRequiredThisLoop(false);
			}
			
			// if we're waiting for an "any key" press then draw the 
			// current message 
			if (isWaitingForKeyPress()) {
				g.setColor(Color.white);
				g.drawString(getMessage(),(800-g.getFontMetrics().stringWidth(getMessage()))/2,250);
				g.drawString("Press any key",(800-g.getFontMetrics().stringWidth("Press any key"))/2,300);
			}
			if (isPausePressed()) {
				g.setColor(Color.white);
				g.drawString("Pause",(800-g.getFontMetrics().stringWidth("Pause"))/2,300);
			}
			g.setColor(Color.white);
			g.drawString("Level "+getLevel(),(800-g.getFontMetrics().stringWidth("Level "+getLevel()))/2,20);
		
			// finally, we've completed drawing so clear up the graphics
			// and flip the buffer over
			g.dispose();
			getStrategy().show();
			
			// resolve the movement of the ship. First assume the ship 
			// isn't moving. If either cursor key is pressed then
			// update the movement appropriately
			getShip().setHorizontalMovement(0);
			
			if ((isLeftPressed()) && (!isRightPressed())) {
				getShip().setHorizontalMovement(-getMoveSpeed());
			} else if ((isRightPressed()) && (!isLeftPressed())) {
				getShip().setHorizontalMovement(getMoveSpeed());
			}
			
			getShip().setVerticalMovement(0);
			
			if ((isUpPressed()) && (!isDownPressed())) {
				getShip().setVerticalMovement(-getMoveSpeed());
			} else if (isDownPressed() && !isUpPressed()) {
				getShip().setVerticalMovement(getMoveSpeed());
			}
			
			// if we're pressing fire, attempt to fire
			if (isFirePressed()) {
				tryToFire();
			}
			if (isFire2Pressed()) {
				tryToFire2();
			}
			
			// finally pause for a bit. Note: this should run us at about
			// 100 fps but on windows this might vary each loop due to
			// a bad implementation of timer
			SystemTimer.sleep(lastLoopTime+10-SystemTimer.getTime());
		}	
	}
	
	@Override
    public void mouseClicked(MouseEvent e) {
    }
 
    @Override
    public void mouseEntered(MouseEvent e) {
    }
 
    @Override
    public void mouseExited(MouseEvent e) {
    }
 
    @Override
    public void mousePressed(MouseEvent e) {
    	if(e.getButton() == MouseEvent.BUTTON1) setFirePressed(true);
    	else if(e.getButton() == MouseEvent.BUTTON3) setFire2Pressed(true);
    }
 
    @Override
    public void mouseReleased(MouseEvent e) {
		setFirePressed(false);
    }

	public boolean isFire2Pressed() {
		return Fire2Pressed;
	}

	public void setFire2Pressed(boolean fire2Pressed) {
		Fire2Pressed = fire2Pressed;
	}

	public boolean isPausePressed() {
		return PausePressed;
	}

	public void setPausePressed(boolean pausePressed) {
		PausePressed = pausePressed;
	}

	public boolean isUpPressed() {
		return upPressed;
	}

	public void setUpPressed(boolean upPressed) {
		this.upPressed = upPressed;
	}

	public boolean isDownPressed() {
		return downPressed;
	}

	public void setDownPressed(boolean downPressed) {
		this.downPressed = downPressed;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

    
}