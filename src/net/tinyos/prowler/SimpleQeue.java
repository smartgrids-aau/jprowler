package net.tinyos.prowler;
@SuppressWarnings("rawtypes")
public class SimpleQeue 
{
	int size_ = 0;
	long debug_last_event_ = 0;
	QueueEntry first_ = null;
	class QueueEntry
	{
		Comparable content = null;;
		QueueEntry next=null;
		QueueEntry previous=null;
	}
	
	public int size()
	{
		return size_;
	}
	
	
	@SuppressWarnings("unchecked")
	public void add(Comparable element)
	{
		QueueEntry new_entry = new QueueEntry();
		new_entry.content = element;
		size_++;
//		System.out.println("Inserting: "+((Event)element));
//		System.out.println(this);
		if(first_ == null)
		{
			first_ = new_entry;
		}
		else
		{
			QueueEntry entry = first_;		
			while(true)
			{
				if(entry.content.compareTo(element)>0)
				{
					new_entry.next = entry;
					new_entry.previous = entry.previous;
					if(entry.previous == null)
						first_ = new_entry;
					else
						entry.previous.next = new_entry;
					entry.previous = new_entry;
					break;
				}
				if(entry.next == null)
				{
					entry.next = new_entry;
					new_entry.previous = entry;
					break;
				}				
				entry = entry.next;
			}
		}
//		System.out.println(this);
		//checkInvareant();
	}
	
	public void clear()
	{
		size_ = 0;
		first_ = null;
	}
	
	public Object first()
	{
		if(size_==0)
			return null;
		return first_.content;
	}
	
	@SuppressWarnings("unchecked")
	public void remove(Object first)
	{
		if(((Comparable)first).compareTo(first_.content)!=0)
			throw new IllegalArgumentException();
		assert(first == first_.content);
		size_--;
		debug_last_event_ = ((Event)first_.content).time;
		
		first_ = first_.next;
		if(first_ != null)
			first_.previous = null;
	}
	
	public Object getAndRemoveFirst()
	{
		if(size_==0)
			return null;
		size_--;
		Object result = first_.content;
		first_ = first_.next;
		if(first_ != null)
			first_.previous = null;
		return result;
	}
	
	public String toString()
	{
		String result = "";
		if(first_ == null)
			return "empty";
		for(QueueEntry node_entry=first_;node_entry.next!=null;node_entry = node_entry.next)			
		{
			{
				if(result=="")
					result = debug_last_event_+": "+((Event)node_entry.content).time;
				else
					result = result + "," +((Event)node_entry.content).time;
			}
		}		
		return result;
	}
	
}
