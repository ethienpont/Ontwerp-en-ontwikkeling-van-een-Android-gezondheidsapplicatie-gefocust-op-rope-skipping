from django.contrib.auth.models import User, Group
from rest_framework import serializers


class UserSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = User
        fields = ['url', 'username', 'email', 'groups']


class DataSerializer(serializers.Serializer):
    fields = ['url', 'time', 'x', 'y', 'z']
    time = serializers.FloatField()
    x = serializers.FloatField()
    y = serializers.FloatField()
    z = serializers.FloatField()