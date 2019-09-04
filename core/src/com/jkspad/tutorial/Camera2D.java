package com.jkspad.tutorial;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * @author John Knight
 * Copyright http://www.jkspad.com
 *
 */
public class Camera2D extends ApplicationAdapter  {

	private static final float WORLD_WIDTH = 1024;
	private static final float WORLD_HEIGHT = 768;
	private static final float MAX_ZOOM_IN = 0.05f;
	private static final float MAX_ZOOM_OUT = 4f;
	private static final float SPEED_ZOOM_SECONDS = .75f;
	private static final float SPEED_ROTATE_SECONDS = 1.5f;
	private static final float SPEED_TRANSLATE_SECONDS = 800f;
	private static float MAX_SHAKE_X = 10;
	private static float MAX_SHAKE_Y = 10;
	private static float MAX_SHAKE_ROTATION = 4;
	private static final float MAX_SHAKE_TIME = 0.5f;

	private static final int KEYS_TEXT_X = 10;
	private static final int KEYS_TEXT_Y = 20;
	private static final int STATUS_TEXT_X = 10;
	private static final int STATUS_TEXT_Y = 470;

	private final Color colCornflowerBlue = new Color(100f/255f, 149f/255f, 237f/255f, 1);
	private SpriteBatch spriteBatch;
	private BitmapFont font;
	private ShaderProgram shader;
	private OrthographicCamera camera;
	private Mesh mesh;
	private Texture texture;

	private float elapsedZoom = 0;
	private float currentRotation = 0;

	// Camera shake members
	private float shakeTime; // a bit like "sexy time"
	private Vector3 storedPosition;
	private float storedRotation;

	private final String VERTEX_SHADER =
			"attribute vec4 a_position;\n"
					+ "attribute vec2 a_texCoord; \n"
					+ "uniform mat4 u_projTrans;\n"
					+ "varying vec2 v_texCoord; \n"
					+ "void main() {\n"
					+ " gl_Position = u_projTrans * a_position;\n"
					+ " v_texCoord =  a_texCoord; \n" +
					"}";

	private final String FRAGMENT_SHADER =
			"#ifdef GL_ES\n" +
					"precision mediump float;\n" +
					"#endif\n"+
					"varying vec2 v_texCoord; \n" +
					"uniform sampler2D u_texture; \n" +
					"void main() \n"+
					"{ \n"+
					" gl_FragColor = texture2D( u_texture, v_texCoord );\n"+
					"} \n";

	protected void createMeshShader() {
		ShaderProgram.pedantic = false;
		shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
		String log = shader.getLog();
		if (!shader.isCompiled()){
			throw new GdxRuntimeException(log);
		}
		if (log!=null && log.length()!=0){
			Gdx.app.log("shader log", log);
		}
	}

	private void createTexture () {
		texture = new Texture("test.png"); // 1024 x 768
	}

	@Override
	public void create () {

		float halfWidth = WORLD_WIDTH / 2;
		float halfHeight = WORLD_HEIGHT / 2;

		mesh = new Mesh(true, 4, 0,
				new VertexAttribute(Usage.Position, 2, "a_position"),
				new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord")
		);

		float[] vertices = {
				-halfWidth, -halfHeight,	// quad bottom left
				0.0f, 1.0f, 				// texture bottom left
				halfWidth, -halfHeight, 	// quad bottom right
				1f, 1.0f, 	    			// texture bottom right
				-halfWidth, halfHeight,		// quad top left
				0.0f, 0.0f, 				// texture top left
				halfWidth, halfHeight,		// quad top right
				1.0f, 0.0f 					// texture top-right

		};

		mesh.setVertices(vertices);
		createTexture();
		createMeshShader();
		font = new BitmapFont();
		spriteBatch = new SpriteBatch();

	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		camera = new OrthographicCamera(width, height);
		camera.zoom = (MAX_ZOOM_OUT - MAX_ZOOM_IN) / 2;
		elapsedZoom = SPEED_ZOOM_SECONDS / 2;
		currentRotation = 0;
	}

	private void showMessage(){
		StringBuilder sb = new StringBuilder();
		sb.append("Pos: ").append(camera.position.x).append(",").append(camera.position.y);
		sb.append("\nZoom: ").append(camera.zoom);
		sb.append("\nRotation (degrees): ").append(currentRotation);
		spriteBatch.begin();
		font.draw(spriteBatch, "Arrows = Move; W/S = Zoom In/Out; A/D = Rotate; R = Reset; SPACE = Shake it!", KEYS_TEXT_X, KEYS_TEXT_Y);
		font.draw(spriteBatch, sb.toString(), STATUS_TEXT_X, STATUS_TEXT_Y);
		spriteBatch.end();
	}

	private void handleKeys(float delta){

		if(Gdx.input.isKeyPressed(Keys.SPACE) && shakeTime == 0){
			startShakingBaby();
		}

		if(shakeTime > 0){
			return; // we are shaking, so that'll be all for now thanks
		}


		if(Gdx.input.isKeyPressed(Keys.W)){
			zoomOut(delta);
		}else if(Gdx.input.isKeyPressed(Keys.S)){
			zoomIn(delta);
		}

		if(Gdx.input.isKeyPressed(Keys.A)){
			rotateLeft(delta);
		}else if(Gdx.input.isKeyPressed(Keys.D)){
			rotateRight(delta);
		}

		if(Gdx.input.isKeyPressed(Keys.LEFT)){
			panLeft(delta);
		}else if(Gdx.input.isKeyPressed(Keys.RIGHT)){
			panRight(delta);
		}

		if(Gdx.input.isKeyPressed(Keys.UP)){
			panUp(delta);
		}else if(Gdx.input.isKeyPressed(Keys.DOWN)){
			panDown(delta);
		}

		if(Gdx.input.isKeyPressed(Keys.R)){
			reset();
		}
	}

	@Override
	public void render () {
		float delta = Gdx.graphics.getDeltaTime();
		handleKeys(delta);
		shakeItBaby(delta);
		camera.update();

		Gdx.gl.glClearColor(colCornflowerBlue.r, colCornflowerBlue.g, colCornflowerBlue.b, colCornflowerBlue.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		Gdx.gl20.glActiveTexture(GL20.GL_TEXTURE0);
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		texture.bind();

		shader.begin();
		shader.setUniformi("u_texture", 0);
		shader.setUniformMatrix("u_projTrans", camera.combined);
		mesh.render(shader, GL20.GL_TRIANGLE_STRIP);
		shader.end();

		showMessage();
	}


	private void reset(){
		resetRotation();

		camera.zoom = MAX_ZOOM_OUT / 2;
		elapsedZoom = SPEED_ZOOM_SECONDS / 2;
		camera.position.x = 0;
		camera.position.y = 0;
	}

	private void startShakingBaby()
	{
		if (this.shakeTime > 0){
			return; // we is already shakin' innit.
		}
		this.shakeTime = MAX_SHAKE_TIME;
		storedPosition = new Vector3(camera.position);
		storedRotation = currentRotation;
	}

	private void shakeItBaby(float delta)
	{
		if (shakeTime == 0)
			return;

		shakeTime -= delta;

		if (shakeTime <= 0)
		{
			shakeTime = 0;
			camera.position.x = storedPosition.x;
			camera.position.y = storedPosition.y;
			camera.position.z = storedPosition.z;
			rotate(-currentRotation + storedRotation);
			return;
		}

		int posModifier = 1;
		int rotModifier = 1;
		if (MathUtils.random(10) >= 5)
			posModifier = -1;
		if (MathUtils.random(10) >= 5)
			rotModifier = -1;

		float posXAmount = MathUtils.random(MAX_SHAKE_X) * posModifier;
		float posYAmount = MathUtils.random(MAX_SHAKE_Y) * posModifier;
		float rotAmount = MathUtils.random(MAX_SHAKE_ROTATION) * rotModifier;

		camera.position.x = storedPosition.x + posXAmount;
		camera.position.y = storedPosition.y + posYAmount;

		rotate(rotAmount);
	}

	private void zoomOut(float delta){
		elapsedZoom  = MathUtils.clamp(elapsedZoom + delta, 0, SPEED_ZOOM_SECONDS);
		camera.zoom = MathUtils.lerp(MAX_ZOOM_IN, MAX_ZOOM_OUT, elapsedZoom / SPEED_ZOOM_SECONDS);
	}

	private void zoomIn(float elapsed){
		elapsedZoom  = MathUtils.clamp(elapsedZoom - elapsed, 0, SPEED_ZOOM_SECONDS);
		camera.zoom = MathUtils.lerp(MAX_ZOOM_IN, MAX_ZOOM_OUT, elapsedZoom / SPEED_ZOOM_SECONDS);
	}

	private void resetRotation(){
		rotate(-currentRotation);
	}

	private void rotate(float degrees){
		camera.rotate(degrees);
		currentRotation += degrees;
	}

	private void rotateLeft(float delta){
		float deltaRotation = 360 * delta / SPEED_ROTATE_SECONDS;
		rotate(deltaRotation);
	}

	private void rotateRight(float delta){
		float deltaRotation = -360 * delta / SPEED_ROTATE_SECONDS;
		rotate(deltaRotation);
	}

	private boolean canPan(){
		return !(camera.zoom > WORLD_WIDTH / camera.viewportWidth);
	}

	private void panLeft(float delta){
		if(canPan()){
			camera.translate(-SPEED_TRANSLATE_SECONDS * delta, 0);
			float scaledViewportWidth = camera.viewportWidth * camera.zoom;
			float maxX = WORLD_WIDTH / 2 - scaledViewportWidth /2;
			float minX = -maxX;
			camera.position.x = MathUtils.clamp(camera.position.x, minX, maxX);
		}
	}

	private void panRight(float delta){
		if(canPan()){
			camera.translate(SPEED_TRANSLATE_SECONDS * delta, 0);
			float scaledViewportWidth = camera.viewportWidth * camera.zoom;
			float maxX = WORLD_WIDTH / 2 - scaledViewportWidth /2;
			float minX = -maxX;
			camera.position.x = MathUtils.clamp(camera.position.x, minX, maxX);
		}
	}

	private void panUp(float delta){
		if(canPan()){
			camera.translate(0, SPEED_TRANSLATE_SECONDS * delta);
			float scaledViewportHeight = camera.viewportHeight * camera.zoom;
			float maxY = WORLD_HEIGHT / 2 - scaledViewportHeight /2;
			float minY = -maxY;
			camera.position.y = MathUtils.clamp(camera.position.y, minY, maxY);
		}
	}

	private void panDown(float delta){
		if(canPan()){
			camera.translate(0, -SPEED_TRANSLATE_SECONDS * delta);
			float scaledViewportHeight = camera.viewportHeight * camera.zoom;
			float maxY = WORLD_HEIGHT / 2 - scaledViewportHeight /2;
			float minY = -maxY;
			camera.position.y = MathUtils.clamp(camera.position.y, minY, maxY);
		}
	}
}
