package com.intel.otc.brillo.examples;

import android.os.RemoteException;
import android.pio.I2cDevice;
import android.pio.PeripheralManagerService;
import android.system.ErrnoException;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/*
 * Ported from Grove LCD RGB Backlight driver developed by Seeed-Studio
 * https://github.com/Seeed-Studio/Grove_LCD_RGB_Backlight
 */
public class LcdRgbBacklight {
    private static final String TAG = LcdRgbBacklight.class.getSimpleName();
    private static final String Name = "LCD RGB Backlight";
    private static final String I2C = "I2C6";

    // Device I2C Address
    private static final byte LCD_ADDRESS = (0x7c >> 1),
                              RGB_ADDRESS = (0xc4 >> 1);
    // RGB registers
    private static final byte REG_MODE1  = 0x00,
                              REG_MODE2  = 0x01,
                              REG_BLUE   = 0x02,
                              REG_GREEN  = 0x03,
                              REG_RED    = 0x04,
                              REG_OUTPUT = 0x08;
    // commands
    private static final int LCD_CLEARDISPLAY   = 0x01,
                             LCD_RETURNHOME     = 0x02,
                             LCD_ENTRYMODESET   = 0x04,
                             LCD_DISPLAYCONTROL = 0x08,
                             LCD_CURSORSHIFT    = 0x10,
                             LCD_FUNCTIONSET    = 0x20,
                             LCD_SETCGRAMADDR   = 0x40,
                             LCD_SETDDRAMADDR   = 0x80;
    // flags for display entry mode
    private static final byte LCD_ENTRYRIGHT = 0x00,
                              LCD_ENTRYLEFT  = 0x02,
                              LCD_ENTRYSHIFTINCREMENT = 0x01,
                              LCD_ENTRYSHIFTDECREMENT = 0x00;
    // flags for display on/off control
    private static final byte LCD_DISPLAYON  = 0x04,
                              LCD_DISPLAYOFF = 0x00,
                              LCD_CURSORON   = 0x02,
                              LCD_CURSOROFF  = 0x00,
                              LCD_BLINKON    = 0x01,
                              LCD_BLINKOFF   = 0x00;
    // flags for display/cursor shift
    private static final byte LCD_DISPLAYMOVE = 0x08,
                              LCD_CURSORMOVE  = 0x00,
                              LCD_MOVERIGHT   = 0x04,
                              LCD_MOVELEFT    = 0x00;
    // flags for function set
    public static final byte LCD_8BITMODE = 0x10,
                             LCD_4BITMODE = 0x00,
                             LCD_2LINE    = 0x08,
                             LCD_1LINE    = 0x00,
                             LCD_5x10DOTS = 0x04,
                             LCD_5x8DOTS  = 0x00;

    private PeripheralManagerService mService;
    private byte _displayfunction = LCD_1LINE | LCD_4BITMODE | LCD_5x8DOTS,
                 _displaycontrol  = LCD_DISPLAYON | LCD_CURSOROFF | LCD_BLINKOFF,
                 _displaymode     = LCD_ENTRYLEFT | LCD_ENTRYSHIFTDECREMENT;

    public LcdRgbBacklight() {
        mService = new PeripheralManagerService();
    }

    public void begin(int cols, int lines, byte dotsize) {
        Log.d(TAG, "Initialize " + Name + "...");

        if (lines > 1) {
            _displayfunction |= LCD_2LINE;
        }
        // for some 1 line displays you can select a 10 pixel high font
        if ((dotsize != 0) && (lines == 1)) {
            _displayfunction |= LCD_5x10DOTS;
        }

        // SEE PAGE 45/46 FOR INITIALIZATION SPECIFICATION!
        // according to datasheet, we need at least 40ms after power rises above 2.7V
        // before sending commands. Arduino can turn on way befer 4.5V so we'll wait 50
        delayMicroseconds(50000);

        // this is according to the hitachi HD44780 datasheet
        // page 45 figure 23
        // Send function set command sequence
        command(LCD_FUNCTIONSET | _displayfunction);
        delayMicroseconds(4500);  // wait more than 4.1ms
        // second try
        command(LCD_FUNCTIONSET | _displayfunction);
        delayMicroseconds(150);
        // third go
        command(LCD_FUNCTIONSET | _displayfunction);
        // finally, set # lines, font size, etc.
        command(LCD_FUNCTIONSET | _displayfunction);

        // turn the display on with no cursor or blinking default
        display();
        // clear it off
        clear();

        // Initialize to default text direction (for romance languages)
        // set the entry mode
        command(LCD_ENTRYMODESET | _displaymode);

        // backlight init
        setReg(REG_MODE1, 0);
        // set LEDs controllable by both PWM and GRPPWM registers
        setReg(REG_OUTPUT, 0xFF);
        // set MODE2 values
        // 0010 0000 -> 0x20  (DMBLNK to 1, ie blinky mode)
        setReg(REG_MODE2, 0x20);

        setRGB(255, 255, 255);
    }

    public synchronized void write(String s) {
        try {
            I2cDevice dev = mService.openI2cDevice(I2C, LCD_ADDRESS);
            for (byte ch : s.getBytes())
                dev.writeRegByte(0x40, ch);
            dev.close();
        } catch (RemoteException | ErrnoException e) {
            Log.e(TAG, "Exception on writing String '" + s + "' to LCD");
        }
    }

    public synchronized void clear() {
        command(LCD_CLEARDISPLAY);        // clear display, set cursor position to zero
        delayMicroseconds(2000);          // this command takes a long time!
    }

    public synchronized void home() {
        command(LCD_RETURNHOME);        // set cursor position to zero
        delayMicroseconds(2000);        // this command takes a long time!
    }

    public synchronized void noDisplay() {
        _displaycontrol &= ~LCD_DISPLAYON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public synchronized void display() {
        _displaycontrol |= LCD_DISPLAYON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public synchronized void setCursor(int col, int row) {
        col = (row == 0 ? col|0x80 : col|0xc0);
        command(col);
    }

    public synchronized void noCursor() {
        _displaycontrol &= ~LCD_CURSORON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public synchronized void cursor() {
        _displaycontrol |= LCD_CURSORON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public synchronized void noBlink() {
        _displaycontrol &= ~LCD_BLINKON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public synchronized void blink() {
        _displaycontrol |= LCD_BLINKON;
        command(LCD_DISPLAYCONTROL | _displaycontrol);
    }

    public synchronized void scrollDisplayLeft() {
        command(LCD_CURSORSHIFT | LCD_DISPLAYMOVE | LCD_MOVELEFT);
    }

    public synchronized void scrollDisplayRight() {
        command(LCD_CURSORSHIFT | LCD_DISPLAYMOVE | LCD_MOVERIGHT);
    }

    public synchronized void leftToRight() {
        _displaymode |= LCD_ENTRYLEFT;
        command(LCD_ENTRYMODESET | _displaymode);
    }

    public synchronized void rightToLeft() {
        _displaymode &= ~LCD_ENTRYLEFT;
        command(LCD_ENTRYMODESET | _displaymode);
    }

    public synchronized void autoscroll() {
        _displaymode |= LCD_ENTRYSHIFTINCREMENT;
        command(LCD_ENTRYMODESET | _displaymode);
    }

    public synchronized void noAutoscroll() {
        _displaymode &= ~LCD_ENTRYSHIFTINCREMENT;
        command(LCD_ENTRYMODESET | _displaymode);
    }

    private void command(int value) {
        try {
            I2cDevice dev = mService.openI2cDevice(I2C, LCD_ADDRESS);
            dev.writeRegByte(0x80, value);
            dev.close();
        } catch (RemoteException | ErrnoException e) {
            Log.e(TAG, "Exception on writing " + value + " to LCD");
        }
    }

    public synchronized void setRGB(int r, int g, int b) {
        setReg(REG_RED, r);
        setReg(REG_GREEN, g);
        setReg(REG_BLUE, b);
    }

    private void setReg(byte addr, int dta) {
        try {
            I2cDevice dev = mService.openI2cDevice(I2C, RGB_ADDRESS);
            dev.writeRegByte(addr, dta);
            dev.close();
        } catch (RemoteException | ErrnoException e) {
            Log.e(TAG, "Exception on writing RGB[" + addr + "]");
        }
    }

    private void delayMicroseconds(int msec) {
        try {
            TimeUnit.MICROSECONDS.sleep(msec);
        } catch (InterruptedException e) {
            // Ignore sleep interruption.
        }
    }
}
