Here are some performance results given the following test code:

// had to make the class static otherwise inner class couldn't get instanciated properly

    public static class Call {
        String phoneNumber;
        Long date;
        public Call() {
        }
        public void setNumber(String number) {
          phoneNumber = number;
        }
        public void setDate(Long date) {
         this.date = date; 
           //DateUtils.formatDateTime(MyActivity.this.getApplicationContext(), date, DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE);
        }
        public void reset() {
        	this.phoneNumber = null;
        	this.date = null;
        }
        public String toString() {
          return "Called " + phoneNumber + " at " + date;
        }
      }
    
    public void test() {
    	for (int i = 0; i < 3; i++) {
			testLoader1();
			testLoader2();
			testLoader3();
    	}
    }
    
    public void testLoader1() {
    	testLoader(false);
    }

    public void testLoader2() {
    	testLoader(true);
    }

    public void testLoader3() {
    	long start = System.currentTimeMillis();
    	Cursor cursor =   
    		  getContentResolver().query(CallLog.Calls.CONTENT_URI,
    		                             null, null, null, null);
    	int counter = 0;
    	while (cursor.moveToNext()) {
    		  String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
    		  Long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
      	      counter++;
    		  String s = "Called " + number + " at " + date;
    	}
		cursor.close(); 
     	Log.i(LOG_TAG, "Took " + (System.currentTimeMillis() - start) + "ms. 3 " + counter);
    }

    public void testLoader(boolean reuse) {
    	int counter = 0;
    	long start = System.currentTimeMillis();
    	Reader<Call> calls = new Reader<Call>(Call.class, this.getApplicationContext(), CallLog.Calls.CONTENT_URI, reuse);
    	 for (Call call : calls) {
    	     call.toString();
    	     counter++;
    	 }    	
     	Log.i(LOG_TAG, "Took " + (System.currentTimeMillis() - start) + "ms. " + reuse + " " + counter);
    }


* default android framework
Took 507ms. 3 500
Took 522ms. 3 500
Took 496ms. 3 500

* without reuse
Took 6038ms. false 500
Took 5574ms. false 500
Took 5485ms. false 500

* with memory reuse (code supporting this feature was removed)
Took 5441ms. true 500
Took 5593ms. true 500
Took 5463ms. true 500

Conclusions
* memory reuse doesn't help, so I took the code out
* performance is about x10 slower compared to raw Android Cursor usage due to reflection
