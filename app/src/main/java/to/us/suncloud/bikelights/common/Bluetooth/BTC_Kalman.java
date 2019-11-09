package to.us.suncloud.bikelights.common.Bluetooth;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import to.us.suncloud.bikelights.common.ByteMath;

public class BTC_Kalman {
    private int n_obs = 2;
    private int n_sta = 3;

    private float Q;
    private List<List<Float>> R; // n_obs x n_obs
    private List<List<Float>> P; // n_sta x n_sta

    public BTC_Kalman(int n_obs, int n_sta, float Q, List<List<Float>> R, List<List<Float>> P) {
        this.n_obs = n_obs;
        this.n_sta = n_sta;
        this.Q = Q;
        this.R = R;
        this.P = P;
    }

    public BTC_Kalman(SharedPreferences preferences) {
        // Initialize the Kalman object using a preferences object (from this application...don't get smart with me)
        //        this.n_obs = Integer.parseInt(preferences.getString("kalman_obs", "100"));
        //        this.n_sta = Integer.parseInt(preferences.getString("kalman_sta", "100"));

        // Extract Q
        Q = Float.parseFloat(preferences.getString("kalman_q", "100.0"));

        // Extract P diagonal values (you know...the important bits)
        List<Float> PDiag = new ArrayList<>(n_sta); // Initialize the capacity (not the size!)
        PDiag.add(Float.valueOf(preferences.getString("kalman_p_1", "0")));
        PDiag.add(Float.parseFloat(preferences.getString("kalman_p_2", "1")));
        PDiag.add(Float.parseFloat(preferences.getString("kalman_p_3", "10")));

        // Extract R diagonal values
        List<Float> RDiag = new ArrayList<>(n_obs);
        RDiag.add(Float.parseFloat(preferences.getString("kalman_r_1", "0.00001")));
        RDiag.add(Float.parseFloat(preferences.getString("kalman_r_2", "0.00001")));

        // Now that we have extracted the actually interesting values from the preferences object, populate the diagonal of an n_sta (P) and an n_obs (R) matrix with the values we just got
        // Make P
        P = new ArrayList<>(n_sta);
        for (int row = 0; row < n_sta; row++) {
            // Create a row of P
            List<Float> thisPRow = new ArrayList<>(n_sta);

            for (int col = 0; col < n_sta; col++) {
                // If we are on the diagonal (row and column are equal), then populate from our newly extracted values from the preferences.  Otherwise, just add a 0.
                if (row == col) {
                    // We are on a diagonal
                    thisPRow.add(PDiag.get(row));
                } else {
                    // We are not on a diagonal
                    thisPRow.add(0f);
                }
            }

            // Add the newly populated row
            P.add(thisPRow);
        }

        // Make R
        R = new ArrayList<>(n_obs);
        for (int row = 0; row < n_obs; row++) {
            // Create a row of R
            List<Float> thisRRow = new ArrayList<>(n_obs);

            for (int col = 0; col < n_obs; col++) {
                // If we are on the diagonal (row and column are equal), then populate from our newly extracted values from the preferences.  Otherwise, just add a 0.
                if (row == col) {
                    // We are on a diagonal
                    thisRRow.add(RDiag.get(row));
                } else {
                    // We are not on a diagonal
                    thisRRow.add(0f);
                }
            }

            // Add the newly populated row
            R.add(thisRRow);
        }
    }


    public int getN_obs() {
        return n_obs;
    }

    public int getN_sta() {
        return n_sta;
    }

    public float getQ() {
        return Q;
    }

    public List<List<Float>> getR() {
        return R;
    }

    public List<List<Float>> getP() {
        return P;
    }

    public BluetoothByteList toByteList() {
        // Convert this BTC_Kalman object to a byte list
        // Create the byte list that will eventually be sent
        BluetoothByteList rawByteList = new BluetoothByteList(BluetoothByteList.ContentType.Kalman, false);

        // First, send the BTC_Kalman meta data (number of observed and state variables)
        byte numVarsByte = 0;
        numVarsByte = ByteMath.putDataToByte(numVarsByte, (byte) n_obs, 4, 4); // TODO: Check with online compiler, can we just convert an int to a byte?  Probably because it's below 127, but still may want to check...
        numVarsByte = ByteMath.putDataToByte(numVarsByte, (byte) n_sta, 4, 0);
        rawByteList.addByte(numVarsByte);

        // Next, send Q
        List<Byte> qBytes = ByteMath.putFloatToByteArray(Q);
        rawByteList.addBytes(qBytes);

        // Next, send the matrix R
        for (int row = 0; row < n_obs; row++) {
            List<Float> thisRRow = R.get(row);
            for (int col = 0; col < n_obs; col++) {
                // Send each value of R, one at a time
                rawByteList.addBytes(ByteMath.putFloatToByteArray(thisRRow.get(col)));
            }
        }

        // Finally, send the matrix P
        for (int row = 0; row < n_sta; row++) {
            List<Float> thisPRow = P.get(row);
            for (int col = 0; col < n_sta; col++) {
                // Send each value of P, one at a time
                rawByteList.addBytes(ByteMath.putFloatToByteArray(thisPRow.get(col)));
            }
        }

        // Return the byte list so that it can be set
        return rawByteList;
    }

    static public BTC_Kalman fromByteList(BluetoothByteList rawByteList) {
        // Extract a BTC_Kalman object from the incoming raw byte list

        rawByteList.startReading(); // Set the pointer to the beginning of the byte list

        // First, get the BTC_Kalman meta data (number of observed and state variables
        byte numVarsByte = rawByteList.getByte(); // Get the first byte, which contains both n_obs and n_sta
        int n_obs = ByteMath.getNIntFromByte(numVarsByte, 4, 4);
        int n_sta = ByteMath.getNIntFromByte(numVarsByte, 4, 0);

        // Next, get Q, which is a float (in 4 bytes)
        List<Byte> qBytes = rawByteList.getNextBytes(4);
        float Q = ByteMath.getFloatFromByteArray(qBytes);

        // Next, get the matrix R
        List<List<Float>> R = new ArrayList<List<Float>>(n_obs * n_obs);
        for (int row = 0; row < n_obs; row++) {
            List<Float> thisRRow = new ArrayList<Float>(n_obs); // Create the row to be populated
            for (int col = 0; col < n_obs; col++) {
                List<Byte> thisRBytes = rawByteList.getNextBytes(4); // Get the next set of 4 bytes
                thisRRow.add(ByteMath.getFloatFromByteArray(thisRBytes)); // Interpret the bytes as a single float
            }
            R.add(thisRRow); // Add the newly populated row to the matrix R
        }

        // Next, get the matrix P
        List<List<Float>> P = new ArrayList<List<Float>>(n_sta * n_sta);
        for (int row = 0; row < n_sta; row++) {
            List<Float> thisPRow = new ArrayList<Float>(n_sta); // Create the row to be populated
            for (int col = 0; col < n_sta; col++) {
                List<Byte> thisPBytes = rawByteList.getNextBytes(4); // Get the next set of 4 bytes
                thisPRow.add(ByteMath.getFloatFromByteArray(thisPBytes)); // Interpret the bytes as a single float
            }
            P.add(thisPRow); // Add the newly populated row to the matrix R
        }

        // Finally, create the BTC_Kalman object, and return it
        return new BTC_Kalman(n_obs, n_sta, Q, R, P);
    }
}
