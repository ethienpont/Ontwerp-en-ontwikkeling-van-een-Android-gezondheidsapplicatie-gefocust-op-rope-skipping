from django.contrib import admin
from django.urls import include, path
from .views import *

urlpatterns = [
    path('calc', YourView.as_view())
]