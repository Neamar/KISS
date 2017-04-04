package fr.neamar.kiss.preference;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.LinearLayout;

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerPalette;
import com.android.colorpicker.ColorPickerSwatch.OnColorSelectedListener;

import fr.neamar.kiss.R;


public class ColorPreference extends DialogPreference implements OnColorSelectedListener {
	private ColorPickerPalette palette;
	
	private int   selectedColor;
	private int[] availableColors;
	
	
	public ColorPreference(Context context) {
		this(context, null);
	}
	
	public ColorPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		this.setDialogLayoutResource(R.layout.pref_color);
		
		this.availableColors = new int[] {
				0xFF4CAF50, 0xFFD32F2F, 0xFFC2185B, 0xFF7B1FA2,
				0xFF512DA8, 0xFF303F9F, 0xFF1976D2, 0xFF0288D1,
				0xFF0097A7, 0xFF00796B, 0xFF388E3C, 0xFF689F38,
				0xFFAFB42B, 0xFFFBC02D, 0xFFFFA000, 0xFFF57C00,
				0xFFE64A19, 0xFF5D4037, 0xFF616161, 0xFF455A64,
				0xFF000000
		};
		this.selectedColor = this.availableColors[0];
		
		// Override default selected color value with value from preference XML
		if(attrs != null) {
			String value = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "defaultValue");
			if(value != null) {
				this.selectedColor = Color.parseColor(value);
			}
		}
	}
	
	protected void drawPalette() {
		if(this.palette != null) {
			this.palette.drawPalette(this.availableColors, this.selectedColor);
		}
	}
	
	@Override
    public void onColorSelected(int color) {
        if(color != this.selectedColor) {
        	if(!this.callChangeListener(color)) {
        		return;
        	}
        	
            this.selectedColor = color;
            this.persistString(String.format("#%08X", this.selectedColor));
            
            // Redraw palette to show checkmark on newly selected color before dismissing
            this.drawPalette();
        }
        
        // Close the dialog
        this.getDialog().dismiss();
    }
	
	@Override
	protected View onCreateDialogView() {
		// Create layout from bound resource
		final View view = super.onCreateDialogView();
		
		// Configure the color picker
		this.palette = (ColorPickerPalette) view.findViewById(R.id.colorPicker);
		this.palette.init(ColorPickerDialog.SIZE_SMALL, 4, this);
		
		// Reconfigure color picker based on the available space
		view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			private boolean ignoreNextUpdate = false;
			
			public void onGlobalLayout() {
				if(this.ignoreNextUpdate) {
					this.ignoreNextUpdate = false;
					return;
				}
				
				// Calculate number of swatches to display
				int swatchSize = ColorPreference.this.palette.getResources().getDimensionPixelSize(com.android.colorpicker.R.dimen.color_swatch_small);
				ColorPreference.this.palette.init(ColorPickerDialog.SIZE_SMALL, (view.getWidth() - 10) / swatchSize, ColorPreference.this);
				
				// Cause redraw and (by extension) also a layout recalculation
				this.ignoreNextUpdate = true;
				ColorPreference.this.drawPalette();
			}
		});
		
		// Bind click events from the custom color values
		Button button1 = (Button) view.findViewById(R.id.colorTransparentDark);
		button1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ColorPreference.this.onColorSelected(0xAA000000);
			}
		});
		Button button2 = (Button) view.findViewById(R.id.colorTransparentWhite);
		button2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ColorPreference.this.onColorSelected(0xAAFFFFFF);
			}
		});
		Button button3 = (Button) view.findViewById(R.id.colorTransparent);
		button3.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ColorPreference.this.onColorSelected(0x00000000);
			}
		});
		
		return view;
	}
	
	@Override
	protected void onBindDialogView(View view) {
		android.util.Log.i("ColorPreference", "View Width:  " + view.getWidth() + " | " + view.getMeasuredWidth());
		// Set selected color value based on the actual color value currently used
		// (but fall back to default from XML)
		this.selectedColor = Color.parseColor(
				this.getPersistedString(String.format("#%08X", this.selectedColor))
		);
		
		this.drawPalette();
	}
}
