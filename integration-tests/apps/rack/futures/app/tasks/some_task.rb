require 'torquebox-messaging'

class SomeTask < TorqueBox::Messaging::Task

  def initialize
    @backchannel = TorqueBox::Messaging::Queue.new( '/queue/backchannel' )
  end
  
  def foo(payload = { })
    @backchannel.publish( 'release' )
    puts 'CALLED'
    'bar'
  end
  
  def with_status(payload={ })
    future.status = '1'
    future.status = '2'
    @backchannel.publish( 'release' )
    @backchannel.receive( :timeout => 1_000 )

  end
    

  def error(payload = { })
    @backchannel.publish( 'release' )
    raise Exception.new('blah')
  end
end
