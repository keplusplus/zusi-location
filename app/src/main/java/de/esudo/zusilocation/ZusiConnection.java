package de.esudo.zusilocation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;

// TODO: Refactor to java.util.concurrent
public class ZusiConnection extends AsyncTask<ZusiConnection.ConnectionParams, ZusiConnection.DataUpdate, Boolean> {
    private final LocationManager mLocMgr;
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;
    private Socket mSocket;

    public ZusiConnection(Context context) {
        mContext = context;
        mLocMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    protected void onPreExecute() {
        Toast.makeText(mContext, "Data fetch task started", Toast.LENGTH_SHORT).show();
        try {
            mLocMgr.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false,
                        false, false, false, Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
        } catch(SecurityException e) {
            e.printStackTrace();
            cancel(true);
        } catch(IllegalArgumentException ignored) {

        }
    }

    @Override
    protected Boolean doInBackground(ConnectionParams... params) {
        Log.d(MainActivity.TAG, "Started background task");
        try {
            mSocket = new Socket();
            try {
                mSocket.connect(new InetSocketAddress(params[0].getHost(), params[0].getPort()), 8000);
            } catch(SocketTimeoutException e) {
                e.printStackTrace();
                return false;
            }

            InputStream inputStream = mSocket.getInputStream();
            OutputStream outputStream = mSocket.getOutputStream();

            ZusiReader zr = new ZusiReader(inputStream);

            char[] id = params[0].getId();
            byte[] helloCmd = { 0x00, 0x00, 0x00, 0x00,
                                0x01, 0x00,
                                    0x00, 0x00, 0x00, 0x00,
                                    0x01, 0x00,
                                        0x04, 0x00, 0x00, 0x00, 0x01, 0x00, 0x02, 0x00,
                                        0x04, 0x00, 0x00, 0x00, 0x02, 0x00, 0x02, 0x00,
                                        0x17, 0x00, 0x00, 0x00, 0x03, 0x00, 'Z', 'u', 's', 'i', 'L', 'o', 'c', 'a', 't', 'i', 'o', 'n', '-', (byte) id[0], (byte) id[1], (byte) id[2], (byte) id[3], (byte) id[4], (byte) id[5], (byte) id[6], (byte) id[7],
                                        0x05, 0x00, 0x00, 0x00, 0x04, 0x00, '2', '.', '0',
                                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

            outputStream.write(helloCmd);
            Log.d(MainActivity.TAG, "Hello command sent");
            String helloAck = zr.readAsString();
            if(helloAck != null) Log.d(MainActivity.TAG, helloAck);

            byte[] neededDataCmd = {0x00, 0x00, 0x00, 0x00,
                                    0x02, 0x00,
                                        0x00, 0x00, 0x00, 0x00,
                                        0x03, 0x00,
                                            0x00, 0x00, 0x00, 0x00,
                                            0x0A, 0x00,
                                                0x04, 0x00, 0x00, 0x00,
                                                0x01, 0x00,
                                                0x16, 0x00, // hour
                                                0x04, 0x00, 0x00, 0x00,
                                                0x01, 0x00,
                                                0x17, 0x00, // minutes
                                                0x04, 0x00, 0x00, 0x00,
                                                0x01, 0x00,
                                                0x18, 0x00, // seconds
                                                0x04, 0x00, 0x00, 0x00,
                                                0x01, 0x00,
                                                0x2F, 0x00, // x
                                                0x04, 0x00, 0x00, 0x00,
                                                0x01, 0x00,
                                                0x30, 0x00, // y
                                                0x04, 0x00, 0x00, 0x00,
                                                0x01, 0x00,
                                                0x31, 0x00, // z
                                                0x04, 0x00, 0x00, 0x00,
                                                0x01, 0x00,
                                                0x32, 0x00, // UTM ref x
                                                0x04, 0x00, 0x00, 0x00,
                                                0x01, 0x00,
                                                0x33, 0x00, // UTM ref y
                                                0x04, 0x00, 0x00, 0x00,
                                                0x01, 0x00,
                                                0x34, 0x00, // UTM zone
                                                0x04, 0x00, 0x00, 0x00,
                                                0x01, 0x00,
                                                0x35, 0x00, // UTM zone 2
                                                0x04, 0x00, 0x00, 0x00,
                                                0x01, 0x00,
                                                0x4B, 0x00, // days ( 0 =  30.12.1899 )
                                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

            outputStream.write(neededDataCmd);
            Log.d(MainActivity.TAG, "Needed data command sent");
            String neededDataAck = zr.readAsString();
            if(neededDataAck != null) Log.d(MainActivity.TAG, neededDataAck);
            // publishProgress();

            float x = 0, y = 0, z = 0;
            int utmrefx = 0, utmrefy = 0, utmzone1 = 0, utmzone2 = 0;

            Log.d(MainActivity.TAG, "Begin of while loop");
            while(true) {
                boolean locationUpdate = false;
                Node root = zr.read();
                if(root != null) {
                    if(root.getId() == 2) {
                        Node dataFtd = root.findNodeById(10);
                        if(dataFtd != null) {
                            Attribute xa = dataFtd.findAttributeById(47);
                            if(xa != null) {
                                ByteBuffer buffer = ByteBuffer.wrap(xa.getData()).order(ByteOrder.LITTLE_ENDIAN);
                                x = buffer.getFloat();
                                locationUpdate = true;
                            }
                            Attribute ya = dataFtd.findAttributeById(48);
                            if(ya != null) {
                                ByteBuffer buffer = ByteBuffer.wrap(ya.getData()).order(ByteOrder.LITTLE_ENDIAN);
                                y = buffer.getFloat();
                                locationUpdate = true;
                            }
                            Attribute za = dataFtd.findAttributeById(49);
                            if(za != null) {
                                ByteBuffer buffer = ByteBuffer.wrap(za.getData()).order(ByteOrder.LITTLE_ENDIAN);
                                z = buffer.getFloat();
                                locationUpdate = true;
                            }
                            Attribute utmrefxa = dataFtd.findAttributeById(50);
                            if(utmrefxa != null) {
                                ByteBuffer buffer = ByteBuffer.wrap(utmrefxa.getData()).order(ByteOrder.LITTLE_ENDIAN);
                                utmrefx = (int) buffer.getFloat();
                                locationUpdate = true;
                            }
                            Attribute utemrefya = dataFtd.findAttributeById(51);
                            if(utemrefya != null) {
                                ByteBuffer buffer = ByteBuffer.wrap(utemrefya.getData()).order(ByteOrder.LITTLE_ENDIAN);
                                utmrefy = (int) buffer.getFloat();
                                locationUpdate = true;
                            }
                            Attribute utmzone1a = dataFtd.findAttributeById(52);
                            if(utmzone1a != null) {
                                ByteBuffer buffer = ByteBuffer.wrap(utmzone1a.getData()).order(ByteOrder.LITTLE_ENDIAN);
                                utmzone1 = (int) buffer.getFloat();
                                locationUpdate = true;
                            }
                            Attribute utmzone2a = dataFtd.findAttributeById(53);
                            if(utmzone2a != null) {
                                ByteBuffer buffer = ByteBuffer.wrap(utmzone2a.getData()).order(ByteOrder.LITTLE_ENDIAN);
                                utmzone2 = (int) buffer.getFloat();
                                locationUpdate = true;
                            }
                        }
                    }
                }

                if(locationUpdate) {
                    StringBuilder updateString = new StringBuilder("LOCATION UPDATE | ");
                    updateString.append("x: ").append(x);
                    updateString.append(" | y: ").append(y);
                    updateString.append(" | z: ").append(z);
                    updateString.append(" | UTM ref x: ").append(utmrefx);
                    updateString.append(" | UTM ref y: ").append(utmrefy);
                    updateString.append(" | UTM zone 1: ").append(utmzone1);
                    updateString.append(" | UTM zone 2: ").append(utmzone2);
                    Log.d(MainActivity.TAG, updateString.toString());

                    double[] latlong = utm2deg(utmzone1, utmrefx * 1000 + x, utmrefy * 1000 + y, utmzone2 > 77);
                    publishProgress(new DataUpdate(DataUpdate.Status.SUCCESS, latlong[0], latlong[1], LocalDateTime.now()));
                }

                if(isCancelled()) {
                    return true;
                }
            }

        } catch (IOException e) {
            Log.w(MainActivity.TAG, "IOException in background task");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onProgressUpdate(ZusiConnection.DataUpdate... update) {
        setMockLocation(update[0].getLatitude(), update[0].getLongitude(), 1);
    }

    @Override
    protected void onCancelled() {
        Log.d(MainActivity.TAG, "Background task destructing...");
        mLocMgr.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
        mLocMgr.removeTestProvider(LocationManager.GPS_PROVIDER);
        try {
            if(mSocket != null && !mSocket.isClosed()) mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ((MainActivity) mContext).deactivate();
    }

    @Override
    protected void onPostExecute(Boolean b) {
        String success = b ? "SUCCESS" : "ERROR";
        Log.d(MainActivity.TAG, "Background task finished gracefully with status " + success);
        if(!b) Toast.makeText(mContext, "Error in server connection", Toast.LENGTH_LONG).show();
        onCancelled();
    }

    public void setMockLocation(double latitude, double longitude, float accuracy) throws SecurityException {
        Location newLocation = new Location(LocationManager.GPS_PROVIDER);
        newLocation.setLatitude(latitude);
        newLocation.setLongitude(longitude);
        newLocation.setAccuracy(accuracy);
        newLocation.setAltitude(0);
        newLocation.setTime(System.currentTimeMillis());
        newLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        mLocMgr.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        mLocMgr.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
        mLocMgr.setTestProviderLocation(LocationManager.GPS_PROVIDER, newLocation);
    }

    private double[] utm2deg(double zone, double easting, double northing, boolean northernHemisphere) {
        if(!northernHemisphere) {
            northing = 10_000_000 - northing;
        }

        // constants
        double a = 6_378_137;
        double e = 0.081819191;
        double e1sq = 0.006739497;
        double k0 = 0.9996;

        // calculation
        double arc = northing / k0;
        double mu = arc / (a * (1 - Math.pow(e, 2) / 4.0 - 3 * Math.pow(e, 4) / 64 - 5 * Math.pow(e, 6) / 256));

        double ei = (1 - Math.pow((1 - e * e), (1 / 2.0))) / (1 + Math.pow((1 - e * e), (1 / 2.0)));

        double ca = 3 * ei / 2 - 27 * Math.pow(ei, 3) / 32.0;
        double cb = 21 * Math.pow(ei, 2) / 16 - 55 * Math.pow(ei, 4) / 32;
        double cc = 151 * Math.pow(ei, 3) / 96;
        double cd = 1097 * Math.pow(ei, 4) / 512;
        double phi1 = mu + ca * Math.sin(2 * mu) + cb * Math.sin(4 * mu) + cc * Math.sin(6 * mu) + cd * Math.sin(8 * mu);

        double n0 = a / Math.pow((1 - Math.pow((e * Math.sin(phi1)), 2)), (1 / 2.0));
        double r0 = a * (1 - e * e) / Math.pow((1 - Math.pow((e * Math.sin(phi1)), 2)), (3 / 2.0));
        double fact1 = n0 * Math.tan(phi1) / r0;

        double _a1 = 500000 - easting;
        double dd0 = _a1 / (n0 * k0);
        double fact2 = dd0 * dd0 / 2;

        double t0 = Math.pow(Math.tan(phi1), 2);
        double Q0 = e1sq * Math.pow(Math.cos(phi1), 2);
        double fact3 = (5 + 3 * t0 + 10 * Q0 - 4 * Q0 * Q0 - 9 * e1sq) * Math.pow(dd0, 4) / 24;

        double fact4 = (61 + 90 * t0 + 298 * Q0 + 45 * t0 * t0 - 252 * e1sq - 3 * Q0 * Q0) * Math.pow(dd0, 6) / 720;

        double lof1 = _a1 / (n0 * k0);
        double lof2 = (1 + 2 * t0 + Q0) * Math.pow(dd0, 3) / 6.0;
        double lof3 = (5 - 2 * Q0 + 28 * t0 - 3 * Math.pow(Q0, 2) + 8 * e1sq + 24 * Math.pow(t0, 2)) * Math.pow(dd0, 5) / 120;

        double _a2 = (lof1 - lof2 + lof3) / Math.cos(phi1);
        double _a3 = _a2 * 180 / Math.PI;

        double latitude = 180 * (phi1 - fact1 * (fact2 + fact3 + fact4)) / Math.PI;
        if(!northernHemisphere) {
            latitude = -latitude;
        }

        double longitude = zone > 0 ? 6 * zone - 183.0 - _a3 : 3.0 - _a3;

        return new double[] {latitude, longitude};
    }

    public static class ConnectionParams {
        private String mHost;
        private int mPort;
        private char[] mId;

        public ConnectionParams(String host, int port, char[] id) {
            this.mHost = host;
            this.mPort = port;
            this.mId = id;
        }

        public String getHost() {
            return mHost;
        }

        public void setHost(String host) {
            this.mHost = host;
        }

        public int getPort() {
            return mPort;
        }

        public void setPort(int port) {
            this.mPort = port;
        }

        public char[] getId() {
            return mId;
        }

        public void setId(char[] id) {
            this.mId = id;
        }
    }

    public static class DataUpdate {
        private final Double mLatitude, mLongitude;
        private final LocalDateTime mIngameTime, mUpdateTime;
        private final Status mStatus;

        private DataUpdate(Status status, double lat, double lon, LocalDateTime igt) {
            this.mStatus = status;
            this.mLatitude = lat;
            this.mLongitude = lon;
            this.mIngameTime = igt;
            this.mUpdateTime = LocalDateTime.now();
        }

        private DataUpdate(Status status) {
            this.mStatus = status;
            this.mLatitude = null;
            this.mLongitude = null;
            this.mIngameTime = null;
            this.mUpdateTime = LocalDateTime.now();
        }

        public double getLatitude() {
            return mLatitude;
        }

        public double getLongitude() {
            return mLongitude;
        }

        public LocalDateTime getIngameTime() {
            return mIngameTime;
        }

        public LocalDateTime getUpdateTime() {
            return mUpdateTime;
        }

        public Status getStatus() {
            return mStatus;
        }

        public enum Status {
            SUCCESS, NO_DATA, FAILED
        }
    }
}
