class Account
  # Public by default.
  def deposit(amount)
    @balance = amount
  end

  protected

  # Callable with a receiver of the same class or a subclass.
  def compare(other)
    other
  end

  private

  # Callable only without an explicit receiver.
  def recompute
    @balance
  end

  public

  # Back to public again.
  def balance
    @balance
  end

  # Applies to this single method only, without flipping the default.
  private def audit
    @balance
  end

  # A trailing public method to prove the `private def` above did not flip the default.
  def close
    @balance = 0
  end
end
