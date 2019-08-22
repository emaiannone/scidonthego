package org.scid.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.ClipboardManager;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.scid.android.gamelogic.ChessParseError;
import org.scid.android.gamelogic.Move;
import org.scid.android.gamelogic.Piece;
import org.scid.android.gamelogic.Position;
import org.scid.android.gamelogic.TextIO;

public class EditBoard extends AppCompatActivity {
	private ChessBoardEdit cb;
	private TextView status;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initUI();
		
		Intent i = getIntent();
		Position pos = null;
		try {
			pos = TextIO.readFEN(i.getAction());
		} catch (ChessParseError e) {
			pos = e.pos;
		}
        if (pos != null)
            cb.setPosition(pos);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		ChessBoardEdit oldCB = cb;
		String statusStr = status.getText().toString();
        initUI();
        cb.cursorX = oldCB.cursorX;
        cb.cursorY = oldCB.cursorY;
        cb.cursorVisible = oldCB.cursorVisible;
        cb.setPosition(oldCB.pos);
        cb.setSelection(oldCB.selectedSquare);
        status.setText(statusStr);
	}

	private void initUI() {
		setContentView(R.layout.editboard);
		cb = findViewById(R.id.eb_chessboard);
        status = findViewById(R.id.eb_status);
		Button okButton = findViewById(R.id.eb_ok);
		Button cancelButton = findViewById(R.id.eb_cancel);

		okButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendBackResult();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		status.setFocusable(false);
        cb.setFocusable(true);
        cb.requestFocus();
        cb.setClickable(true);
        final GestureDetector gd = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            public boolean onDown(MotionEvent e) {
                return false;
            }
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                cb.cancelLongPress();
                return true;
            }
            public boolean onSingleTapUp(MotionEvent e) {
                cb.cancelLongPress();
                handleClick(e);
                return true;
            }
            public boolean onDoubleTapEvent(MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP)
                    handleClick(e);
                return true;
            }
            private void handleClick(MotionEvent e) {
                int sq = cb.eventToSquare(e);
		            Move m = cb.mousePressed(sq);
                if (m != null)
		                doMove(m);
		            }
        });
        cb.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gd.onTouchEvent(event);
			}
		});
        cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
        	public void onTrackballEvent(MotionEvent event) {
        		Move m = cb.handleTrackballEvent(event);
        		if (m != null) {
        			doMove(m);
        		}
        	}
        });
        cb.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				showDialog(EDIT_DIALOG);
				return true;
			}
		});
	}
	
	private void doMove(Move m) {
		if (m.to < 0) {
			if ((m.from < 0) || (cb.pos.getPiece(m.from) == Piece.EMPTY)) {
				cb.setSelection(m.to);
				return;
			}
		}
		Position pos = new Position(cb.pos);
		int piece = Piece.EMPTY;
		if (m.from >= 0) {
			piece = pos.getPiece(m.from);
		} else {
			piece = -(m.from + 2);
		}
		if (m.to >= 0)
			pos.setPiece(m.to, piece);
		if (m.from >= 0)
			pos.setPiece(m.from, Piece.EMPTY);
		cb.setPosition(pos);
        if (m.from >= 0)
		cb.setSelection(-1);
        else
            cb.setSelection(m.from);
		checkValid();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			sendBackResult();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void sendBackResult() {
		if (checkValid()) {
			setPosFields();
			String fen = TextIO.toFEN(cb.pos);
			setResult(RESULT_OK, (new Intent()).setAction(fen));
		} else {
			setResult(RESULT_CANCELED);
		}
		finish();
	}

	private void setPosFields() {
		setEPFile(getEPFile()); // To handle sideToMove change
		TextIO.fixupEPSquare(cb.pos);
		TextIO.removeBogusCastleFlags(cb.pos);
	}
	
	private int getEPFile() {
		int epSquare = cb.pos.getEpSquare();
		if (epSquare < 0) return 8;
		return Position.getX(epSquare);
	}
	
	private void setEPFile(int epFile) {
		int epSquare = -1;
		if ((epFile >= 0) && (epFile < 8)) {
			int epRank = cb.pos.whiteMove ? 5 : 2;
			epSquare = Position.getSquare(epFile, epRank);
		}
		cb.pos.setEpSquare(epSquare);
	}

	/** Test if a position is valid. */
	private boolean checkValid() {
		try {
			String fen = TextIO.toFEN(cb.pos);
			TextIO.readFEN(fen);
			status.setText("");
			return true;
		} catch (ChessParseError e) {
            status.setText(getParseErrString(e));
		}
		return false;
	}

    private String getParseErrString(ChessParseError e) {
        if (e.resourceId == -1)
            return e.getMessage();
        else
            return getString(e.resourceId);
    }

	static final int EDIT_DIALOG = 0; 
	static final int SIDE_DIALOG = 1; 
	static final int CASTLE_DIALOG = 2; 
	static final int EP_DIALOG = 3;
	static final int MOVCNT_DIALOG = 4;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case EDIT_DIALOG: {
			CharSequence[] items = {
					getString(R.string.side_to_move),
					getString(R.string.clear_board), getString(R.string.initial_position),
					getString(R.string.castling_flags), getString(R.string.en_passant_file),
					getString(R.string.move_counters),
					getString(R.string.copy_position), getString(R.string.paste_position)
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.edit_board);
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	switch (item) {
			    	case 0: // Edit side to move
			    		showDialog(SIDE_DIALOG);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	case 1: { // Clear board
			    		Position pos = new Position();
			    		cb.setPosition(pos);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	}
			    	case 2: { // Set initial position
			    		try {
			    			Position pos = TextIO.readFEN(TextIO.startPosFEN);
			    			cb.setPosition(pos);
			    			cb.setSelection(-1);
			    			checkValid();
			    		} catch (ChessParseError e) {
			    		}
			    		break;
			    	}
			    	case 3: // Edit castling flags
			    		removeDialog(CASTLE_DIALOG);
			    		showDialog(CASTLE_DIALOG);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	case 4: // Edit en passant file
			    		removeDialog(EP_DIALOG);
			    		showDialog(EP_DIALOG);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	case 5: // Edit move counters
			    		removeDialog(MOVCNT_DIALOG);
			    		showDialog(MOVCNT_DIALOG);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	case 6: { // Copy position
						setPosFields();
			    		String fen = TextIO.toFEN(cb.pos) + "\n";
			    		ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			    		clipboard.setText(fen);
			    		cb.setSelection(-1);
			    		break;
			    	}
			    	case 7: { // Paste position
			    		ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			    		if (clipboard.hasText()) {
			    			String fen = clipboard.getText().toString();
			    			try {
			    				Position pos = TextIO.readFEN(fen);
			    				cb.setPosition(pos);
			    			} catch (ChessParseError e) {
			    				if (e.pos != null)
					    			cb.setPosition(e.pos);
                                Toast.makeText(getApplicationContext(), getParseErrString(e), Toast.LENGTH_SHORT).show();
			    			}
			    			cb.setSelection(-1);
			    			checkValid();
			    		}
			    		break;
			    	}
			    	}
			    }
			});
			return builder.create();
		}
		case SIDE_DIALOG: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_side_to_move_first);
            int selectedItem = (cb.pos.whiteMove) ? 0 : 1;
            builder.setSingleChoiceItems(new String[]{getString(R.string.white), getString(R.string.black)}, selectedItem, new Dialog.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
                    if (id == 0) { // white to move
			        	   cb.pos.setWhiteMove(true);
			        	   checkValid();
			        	   dialog.cancel();
                    } else {
			        	   cb.pos.setWhiteMove(false);
			        	   checkValid();
			        	   dialog.cancel();
			           }
				}
			       });
			return builder.create();
		}
		case CASTLE_DIALOG: {
			CharSequence[] items = {
				getString(R.string.white_king_castle), getString(R.string.white_queen_castle),
				getString(R.string.black_king_castle), getString(R.string.black_queen_castle)
			};
			boolean[] checkedItems = {
					cb.pos.h1Castle(), cb.pos.a1Castle(),
					cb.pos.h8Castle(), cb.pos.a8Castle()
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.castling_flags);
			builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					Position pos = new Position(cb.pos);
					boolean a1Castle = pos.a1Castle();
					boolean h1Castle = pos.h1Castle();
					boolean a8Castle = pos.a8Castle();
					boolean h8Castle = pos.h8Castle();
					switch (which) {
					case 0: h1Castle = isChecked; break;
					case 1: a1Castle = isChecked; break;
					case 2: h8Castle = isChecked; break;
					case 3: a8Castle = isChecked; break;
					}
					int castleMask = 0;
					if (a1Castle) castleMask |= 1 << Position.A1_CASTLE;
					if (h1Castle) castleMask |= 1 << Position.H1_CASTLE;
					if (a8Castle) castleMask |= 1 << Position.A8_CASTLE;
					if (h8Castle) castleMask |= 1 << Position.H8_CASTLE;
					pos.setCastleMask(castleMask);
					cb.setPosition(pos);
					checkValid();
				}
			});
			return builder.create();
		}
		case EP_DIALOG: {
			CharSequence[] items = {
					"A", "B", "C", "D", "E", "F", "G", "H", getString(R.string.none)
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_en_passant_file);
			builder.setSingleChoiceItems(items, getEPFile(), new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	setEPFile(item);
                    dialog.cancel();
			    }
			});
			return builder.create();
		}
		case MOVCNT_DIALOG: {
        	View content = View.inflate(this, R.layout.edit_move_counters, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            
            builder.setView(content);
            builder.setTitle(R.string.edit_move_counters);
            final EditText halfMoveClock = (EditText)content.findViewById(R.id.ed_cnt_halfmove);
            final EditText fullMoveCounter = (EditText)content.findViewById(R.id.ed_cnt_fullmove);
			halfMoveClock.setText(String.format("%d", cb.pos.halfMoveClock));
			fullMoveCounter.setText(String.format("%d", cb.pos.fullMoveCounter));
			final Runnable setCounters = new Runnable() {
				public void run() {
					try {
				        int halfClock = Integer.parseInt(halfMoveClock.getText().toString());
				        int fullCount = Integer.parseInt(fullMoveCounter.getText().toString());
				        cb.pos.halfMoveClock = halfClock;
				        cb.pos.fullMoveCounter = fullCount;
					} catch (NumberFormatException nfe) {
						Toast.makeText(getApplicationContext(), R.string.invalid_number_format, Toast.LENGTH_SHORT).show();
					}
				}
			};
            builder.setPositiveButton("Ok", new Dialog.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					setCounters.run();
				}
            });
			builder.setNegativeButton("Cancel", null);

			final Dialog dialog = builder.create();

			fullMoveCounter.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
						setCounters.run();
						dialog.cancel();
						return true;
					}
					return false;
				}
	        });
			return dialog;
		}
		}
		return null;
	}
}
