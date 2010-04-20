Note this is a fork of  http://code.google.com/p/providerhelper/

Changes:
* experiment #1
** make row class and Builder more independent. The Builder becomes a Loader
** add a way to reuse and reset row instances to avoid memory allocations
* experiment #2
** move Loader into GenericLoader, allow Reader to use a different kind of Loader (e.g. hardcoded)

Here are some performance results given the following test code.

Experiment #1

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

(Tested on my ADP1)

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

First conclusions
* memory reuse doesn't help, so I took the code out
* performance is about x10 slower compared to raw Android Cursor usage due to reflection


Experiment #2

What if we use a hardcoded Loader ?

I.e.

    public static class CallLoader implements Loader<Call> {
		public Call load(Cursor cursor, Call instance) {
  		  instance.setNumber(cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER)));
  		  instance.setDate(cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE)));
		  return instance;
		}
    }
    
    and
    
      	Reader<Call> calls = new Reader<Call>(new CallLoader(), Call.class, this.getApplicationContext(), CallLog.Calls.CONTENT_URI);
    	 for (Call call : calls) {
    	     call.toString();
    	     counter++;
    	 }    	


I've implemented and test it and here are the new performance tests:
* GenericLoader
I/Talkmore(12584): Took 8092ms. 1 500
I/Talkmore(12584): Took 5429ms. 1 500
I/Talkmore(12584): Took 5687ms. 1 500

* hardcoded loader
I/Talkmore(12584): Took 426ms. 2 500
I/Talkmore(12584): Took 680ms. 2 500
I/Talkmore(12584): Took 558ms. 2 500

* default android framework
I/Talkmore(12584): Took 504ms. 3 500
I/Talkmore(12584): Took 368ms. 3 500
I/Talkmore(12584): Took 374ms. 3 500


We get almost best of both world:

* good performance (almost no reflection)
* code easy to read.

I haven't reenabled the memory reuse part of the Loader. Maybe it would be useful now...
Another idea would be to not have POJOs at all, keep a Reader that returns Iterators and let the Builder dynamically return the fields using setters.

class CallCursor {
  Cursor cursor;
  
  Call(Cursor c) {
    this.cursor = cursor;
  }
  
  String getPhoneNumber() {
    return cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
  }

  String getDate() {
    return cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
  }
}

class Reader implements Iterator<T>, Iterable<T> {

   // ...

    public T next() {
        try {
        	T instance = rowClass.newInstance(cursor);
            moreToCome = cursor.moveToNext();
            if ( !moreToCome ) {
            	cursor.close();
            }
            return next;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

Update: 20/04/2010

I've tried to make a combination of the Iterator design and a hardcoded pre-generated Loader class.
I don't mind the one time cost code generation if that allows me to benefit from the coding style. Best of both worlds ? Good enough for me.
I've ran into issues. See LoaderGenerator for code and links.
Yet it seems that a simple ORM generator, a la ibator for ibatis, could be an interesting solution.


